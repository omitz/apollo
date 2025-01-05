#!/usr/bin/env python
#
# This verison uses Tensorflow Lite.  It reduces memory from 967MB to
# 374MB, about 500MB reduction.
#
# TC 2020-02-11 (Tue)

import os
import numpy as np
import pandas as pd
from scipy.spatial.distance import cdist, euclidean, cosine
from glob import glob

# from model import vggvox_model
import tflite_runtime.interpreter as tflite
from wav_reader import get_fft_spectrum
import constants as c

# to save the enrollmenet profiles
import pickle        # TC 2019-12-31 (Tue) --

###############################
## 1.) Load Tensorflow lite
###############################
tflite_model_file = 'saved_model.tflite'
interpreter = tflite.Interpreter (model_path = tflite_model_file)
interpreter.allocate_tensors ()

###############################
## 2.) Compute features for enrollmenet 
###############################

def computeScore (cosineDist):
    degDist = (np.arccos(1 - cosineDist) * 180 / np.pi)
    assert (degDist > 0)
    if degDist > 90:
            degDist = 90.

    ## lerp: from u1 .. u2 to x1 .. x2
    ## x(u) = x1 ((u2-u)/(u2-u1)) + x2 (u-u1)/(u2-u1)
    ## u1=0, u2=90, x1=100, x2=0
    score = 100. * ((90. - degDist)/(90.))

    return score


def build_buckets(max_sec, step_sec, frame_step):
    buckets = {}
    frames_per_sec = int(1/frame_step)
    end_frame = int(max_sec*frames_per_sec)
    step_frame = int(step_sec*frames_per_sec)
    for i in range(0, end_frame+1, step_frame):
        s = i
        s = np.floor((s-7+2)/2) + 1  # conv1
        s = np.floor((s-3)/2) + 1  # mpool1
        s = np.floor((s-5+2)/2) + 1  # conv2
        s = np.floor((s-3)/2) + 1  # mpool2
        s = np.floor((s-3+2)/1) + 1  # conv3
        s = np.floor((s-3+2)/1) + 1  # conv4
        s = np.floor((s-3+2)/1) + 1  # conv5
        s = np.floor((s-3)/2) + 1  # mpool5
        s = np.floor((s-1)/1) + 1  # fc6
        if s > 0:
            buckets[i] = int(s)
    return buckets


def get_embeddings_from_list_file (list_file, max_sec):
    """
    interpreter is a global variable.
    """
    def model_predict (input_data):
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        # TC 2020-02-09 (Sun) -- minimum is about 4 sec of audio
        print (input_data.shape)
        assert (input_data.shape[2] >= 400)
        # if input_data.shape[2] > 400:
        input_data = np.float32 (input_data[:,:,0:400,:])

        interpreter.set_tensor (input_details[0]['index'], input_data)
        interpreter.invoke ()
        tflite_results = interpreter.get_tensor (output_details[0]['index'])
        output_data = np.array (tflite_results)
        return output_data

    buckets = build_buckets(max_sec, c.BUCKET_STEP, c.FRAME_STEP)
    result = pd.read_csv(list_file, delimiter=",")
    result['features'] = result['filename'].apply(
            lambda x: get_fft_spectrum(x, buckets))
    result['embedding'] = result['features'].apply(
        # lambda x: np.squeeze(model.predict(x.reshape(1,*x.shape,1))))
        lambda x: np.squeeze (model_predict (x.reshape (1, *x.shape, 1))))
    return result[['filename','speaker','embedding']]


def preload_vgg_model ():
    #
    # We want to avoid recomputing enrollment data:
    savedEnrollModel="enrollmentModel_Lite.pkl"
    if os.path.isfile (savedEnrollModel):
        ## load enrollment data
        print ("Loading enrollment data from " + savedEnrollModel)
        [enroll_result, enroll_embs, speakers] = pickle.load (
            open (savedEnrollModel, 'rb'))
    else:
        print("Processing enroll samples....")
        enroll_result = get_embeddings_from_list_file (
            c.ENROLL_LIST_FILE, c.MAX_SEC)
        enroll_embs = np.array ([emb.tolist()
                                 for emb in enroll_result['embedding']])
        speakers = enroll_result['speaker']

        ## Save enrollment data
        print ("Saving enrollment data to " + savedEnrollModel)
        pickle.dump ([enroll_result, enroll_embs, speakers],
                     open (savedEnrollModel, 'wb'))
    return (enroll_embs, speakers)


def identify_speaker (audioFile, outCsvFile, doneFile):
    """ enroll_embs, speakers are global variables"""
    print("Testing input audio file....", audioFile)

    ## create a test list file
    TEST_LIST_FILE = "./test_list_file.csv"
    with open(TEST_LIST_FILE, 'w') as f:
        f.write ("filename,speaker\n")
        f.write ("%s,unknown" % audioFile)

    ## extract the embedding of the unknown person
    test_result = get_embeddings_from_list_file (
        TEST_LIST_FILE, c.MAX_SEC)
    test_embs = np.array([emb.tolist() for emb in test_result['embedding']])

    ## compare test samples against enroll samples
    distances = pd.DataFrame(cdist(test_embs, enroll_embs,
                                   metric=c.COST_METRIC), columns=speakers)

    ## Get the mostl likely speaker
    scores = pd.read_csv(TEST_LIST_FILE, delimiter=",",
                         header=0,names=['test_file','test_speaker'])
    scores = pd.concat([scores, distances],axis=1)

    ## TC 2020-03-09 (Mon) --K-NN modification
    scores['result'] = scores[speakers].idxmin(axis=1) # mindist speaker
    # tc_scores = scores.values[:,2:].astype (np.double) # skip test_file
    #                                                    # and
    #                                                    # test_spacker
    #                                                    # columns
    # k = 3   # may not always be robust??
    # tc_idx = np.argpartition (tc_scores, k, axis=1)[:,0:k] # k smallest
    # tc_knearest = [speakers [tc_idxrow].values.tolist()
    #               for tc_idxrow in tc_idx]
    # tc_result = [max(set(tc_l), key = tc_l.count) # majority
    #              for tc_l in tc_knearest]
    # scores['result'] = tc_result

    ## Get some statistics
    scores['minDist'] = scores[speakers].min(axis=1) # min distance
    scores['maxDist'] = scores[speakers].max(axis=1) # max distance

    speaker = scores['result'][0] # TC 2019-12-31 (Tue) --
    minDist = scores['minDist'][0]
    maxDist = scores['maxDist'][0]
    score = computeScore (minDist)
    with open(outCsvFile, 'w') as f:
        f.write ("speaker,minDist,maxDist,score\n")
        f.write ("%s,%f,%f,%d\n" % (speaker, minDist, maxDist, score))
    print (speaker, minDist, maxDist, score)

    # ## Generate the score image:
    # createScoreImg (minDist, outCsvFile.replace (".csv", ".png"))

    ## Indicate everything is done (to avoid race condition with
    ## the front-end GUI
    doneIdentifying (doneFile)


def serverReady ():
    """
    global variable: pidFile
    """
    global pidFile
    if 'pidFile' not in globals():
        pidFile="/tmp/delme.pid"
        print ("using /tmp/delme.pid for debugging")
    
    with open (pidFile, 'w') as f:
        f.write (str(os.getpid()))

def doneIdentifying (doneFile):
    with open (doneFile, 'w') as f:
        f.write (str(os.getpid()))

        
if __name__ == '__main__':
    # get_id_result()

    ## Create vgg network and preload with enrollment data:
    enroll_embs, speakers = preload_vgg_model ()
    serverReady ()
    print ("VGG Speaker Recognition Server Ready")


    ## Instruction: Call "identify_speaker()" and pass a wav file
    ## limited to less than 10 seconds long.
    # Example:
    # identify_speaker ("unknown_eval_set/Aishwarya_Rai_Bachchan_00387.wav",
    #                   "outCsvFile.csv", "done.pid")

