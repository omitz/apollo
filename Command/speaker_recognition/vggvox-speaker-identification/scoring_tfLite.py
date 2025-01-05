#!/usr/bin/env python

import os
import numpy as np
import pandas as pd
from scipy.spatial.distance import cdist, euclidean, cosine
from wav_reader import get_fft_spectrum
import constants_apollo as c
import pickle        # TC 2019-12-31 (Tue) --

## To address the issue with running out of GPU memory
# import tensorflow as tf
# gpus = tf.config.experimental.list_physical_devices('GPU')
# if gpus:
#     try:
#         # Currently, memory growth needs to be the same across GPUs
#         for gpu in gpus:
#             tf.config.experimental.set_memory_growth(gpu, True)
#         logical_gpus = tf.config.experimental.list_logical_devices('GPU')
#         print(len(gpus), "Physical GPUs,", len(logical_gpus), "Logical GPUs")
#     except RuntimeError as e:
#         # Memory growth must be set before GPUs have been initialized
#         print(e)

import tflite_runtime.interpreter as tflite



###############################
## 1.) Load Tensorflow lite
###############################
tflite_model_file = 'data/model/saved_model.tflite'
# interpreter = tf.lite.Interpreter (model_path = tflite_model_file)
interpreter = tflite.Interpreter (model_path = tflite_model_file)
interpreter.allocate_tensors ()


###############
## Test the TensorFlow Lite model on random input data.
###############
# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

input_shape = input_details[0]['shape']
input_data = np.array (np.random.random_sample (input_shape),
                       dtype=input_details[0]['dtype'])
interpreter.set_tensor (input_details[0]['index'], input_data)

interpreter.invoke()
tflite_results = interpreter.get_tensor (output_details[0]['index'])
output_data = np.array (tflite_results)
# print (output_data)

###############################
## 2.) Compute features for enrollmenet 
###############################
def computeScore (cosineDist):
    degDist = (np.arccos(1 - cosineDist) * 180 / np.pi)
    assert (np.all (degDist > 0))
    degDist [degDist > 90] = 90.
    # if degDist > 90:
    #         degDist = 90.

    ## lerp: from u1 .. u2 to x1 .. x2
    ## x(u) = x1 ((u2-u)/(u2-u1)) + x2 (u-u1)/(u2-u1)
    ## u1=0, u2=90, x1=100, x2=0
    score = 100. * ((90. - degDist)/(90.))

    return score


def build_buckets (max_sec, step_sec, frame_step):
    buckets = {}
    frames_per_sec = int (1 / frame_step) # frame = sample = amplitude in time
    end_frame = int (max_sec * frames_per_sec)
    step_frame = int (step_sec * frames_per_sec)
    for i in range (0, end_frame+1, step_frame):
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


def get_embeddings_from_list_file (interpreter, list_file, max_sec):
    def model_predict (input_data):
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        # if input_data.shape[2] < 400:
        #     input_shape = input_details[0]['shape']
        #     input_data = np.array (np.random.random_sample (input_shape),
        #                            dtype=input_details[0]['dtype'])
        # else:
        #     input_data = np.float32 (input_data[:,:,0:400,:])
        # TC 2020-02-09 (Sun) -- minimum is about 4 sec of audio
        # print (input_data.shape)
        assert (input_data.shape[2] >= 400)
        # if input_data.shape[2] > 400:
        input_data = np.float32 (input_data[:,:,0:400,:])

        interpreter.set_tensor (input_details[0]['index'], input_data)
        interpreter.invoke()
        tflite_results = interpreter.get_tensor (output_details[0]['index'])
        output_data = np.array (tflite_results)
        return output_data

    buckets = build_buckets (max_sec, c.BUCKET_STEP, c.FRAME_STEP)
    result = pd.read_csv (list_file, delimiter=",")
    result['features'] = result['filename'].apply(
        lambda x: get_fft_spectrum (x, buckets))

    ## each feature is a fft of dimsion 512xlength
    result['embedding'] = result['features'].apply(
        # lambda x: np.squeeze (model.predict (x.reshape (1, *x.shape, 1))))
        lambda x: np.squeeze (model_predict (x.reshape (1, *x.shape, 1))))
    return result[['filename','speaker','embedding']]



def get_id_result():
    savedEnrollModel="enrollmentModel_Lite.pkl"
    if os.path.isfile (savedEnrollModel):
        ## load enrollment data
        print ("Loading enrollment data from " + savedEnrollModel)
        [enroll_result, enroll_embs, speakers] = pickle.load (
            open (savedEnrollModel, 'rb'))
    else:
        print("Processing enroll samples....")
        enroll_result = get_embeddings_from_list_file (
            interpreter, c.ENROLL_LIST_FILE, c.MAX_SEC)
        enroll_embs = np.array ([emb.tolist()
                                 for emb in enroll_result['embedding']])
        speakers = enroll_result['speaker']

        ## Save enrollment data
        print ("Saving enrollment data to " + savedEnrollModel)
        pickle.dump ([enroll_result, enroll_embs, speakers],
                     open (savedEnrollModel, 'wb'))

    print("Processing test samples....")
    test_result = get_embeddings_from_list_file (
        interpreter, c.TEST_LIST_FILE, c.MAX_SEC)
    test_embs = np.array([emb.tolist() for emb in test_result['embedding']])

    print("Comparing test samples against enroll samples....")
    distances = pd.DataFrame (cdist (test_embs, enroll_embs,
                                     metric=c.COST_METRIC), columns=speakers)
    # print ("distances = ", distances)

    scores = pd.read_csv (c.TEST_LIST_FILE, delimiter=",",
                          header=0,names=['test_file','test_speaker'])
    scores = pd.concat ([scores, distances], axis=1)

    ## TC 2020-03-09 (Mon) --K-NN modification
    scores['result'] = scores[speakers].idxmin(axis=1)
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

    scores['minDist'] = scores[speakers].min(axis=1)
    scores['degScore'] = computeScore (scores['minDist'])
    scores['correct'] = (scores['result'] == scores['test_speaker'])*1.
    print ("degScore ", scores['degScore'])
    print ("Result is ", scores['result']) # TC 2019-12-31 (Tue) --

    print("Writing outputs to [{}]....".format(c.RESULT_FILE))
    result_dir = os.path.dirname (c.RESULT_FILE)
    if not os.path.exists (result_dir):
        os.makedirs (result_dir)
    with open (c.RESULT_FILE, 'w') as f:
        scores.to_csv(f, index=False, float_format='%.7f')


if __name__ == '__main__':
    get_id_result ()
