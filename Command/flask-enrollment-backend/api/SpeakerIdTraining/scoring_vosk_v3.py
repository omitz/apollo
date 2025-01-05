#!/usr/bin/env python
# 
# This version uses VOSK library and SVM but also export SVM to java
#
# TC 2021-02-26 (Fri) --
# TC 2021-01-10 (Sun) --
#
# degScore  0    41.665216
# 1    54.027182
# 2    52.346636
# 3    49.264803
# 4    41.744810
# 5    47.346673
# 6    54.494848
# 7    56.355584
# 8    42.819847
# 9    49.869775
# Name: degScore, dtype: float64
# predicted_spkr is  0    Aishwarya_Rai_Bachchan
# 1             Ewan_McGregor
# 2             Frankie_Muniz
# 3         Haley_Joel_Osment
# 4              Jim_Gaffigan
# 5              Katie_Holmes
# 6         Leonardo_DiCaprio
# 7               Liam_Neeson
# 8             Liza_Minnelli
# 9              Mohammad_Ali

import os
import sys
import argparse
import textwrap
import json
import numpy as np
import pandas as pd
import constants_apollo as c
import pickle        # TC 2019-12-31 (Tue) --

from sklearn.preprocessing import normalize
from time import time
from sklearn.model_selection import GridSearchCV
from sklearn.svm import SVC
from sklearn.naive_bayes import GaussianNB
from sklearn.metrics import confusion_matrix
from sklearn.metrics import classification_report

from pandarallel import pandarallel
import wave
from vosk import Model, KaldiRecognizer, SpkModel, SetLogLevel

pandarallel.initialize()
SetLogLevel(-1)

from sklearn.externals import joblib as skjoblib

import joblib
from joblib import Memory
mem = Memory(cachedir='/tmp/joblib', verbose=1)


###############################
## 1.) load model and opne audio file
###############################
model_path = "model"
spk_model_path = "model-spk"

if not os.path.exists(model_path):
    print ("Please download the model from https://alphacephei.com/vosk/models and unpack as {} in the current folder.".format(model_path))
    exit (1)

if not os.path.exists(spk_model_path):
    print ("Please download the speaker model from https://alphacephei.com/vosk/models and unpack as {} in the current folder.".format(spk_model_path))
    exit (1)

# Large vocabulary free form recognition
model = Model(model_path)
spk_model = SpkModel(spk_model_path)
# rec = KaldiRecognizer(model, spk_model, 16000)


###############################
## 2.) Compute features for enrollmenet 
###############################
def computeScore (cosineDist):
    degDist = (np.arccos(1 - cosineDist) * 180 / np.pi)
    assert (np.all (degDist > 0))
    degDist [degDist > 90] = 90.
    score = 100. * ((90. - degDist)/(90.))
    return score


def get_embeddings_from_list_file (list_file):
    """This routine extracts embeddings from audio files from a text file.
    (list_file).  It is assumed that each audio file is more than
    4-second long.  If the audio is longer than 4 seconds, just use
    the data from the front portion.
    """
    @mem.cache
    def getEmbedding (fn):
        """
        Gets X-vector 
        """
        wf = wave.open (fn, "rb")
        rec = KaldiRecognizer(model, spk_model, 16000)
        # ts = time()
        # te = time()
        # print ('%2.2f sec' % (te-ts))
        

        if wf.getnchannels() != 1 or wf.getsampwidth() != 2 or wf.getcomptype() != "NONE":
            print ("Audio file must be WAV format mono PCM.")
            exit (1)
        assert (wf.getframerate() == 16000)
        # count = 0
        embeddings = []
        while True:
            data = wf.readframes(4000)
            if len(data) == 0:
                break
            # count += 1
            # rec.AcceptWaveform(data)
            if rec.AcceptWaveform(data):
                # print ("*** count = ", count)
                res = json.loads(rec.Result())
                if 'spk' in res:
                    # print ("Text:", res['text'])
                    embeddings.append(res['spk'])
        res = json.loads(rec.FinalResult())
        if 'spk' in res:
            embeddings.append(res['spk'])
        # print ("     len(embeddings) = ", len(embeddings))
        # avgEmbedding = np.mean (np.array(embeddings), axis=0)
        # print ("Text:", res['text'])
        # return avgEmbedding
        # return res['spk']
        if len (embeddings) == 0:
            print ("WARNNING !**** failed to extract speaker for file " + fn, flush=True)
            1/0
            return np.zeros(128)
        if len (embeddings) > 1:
            # avgEmbedding = np.mean (np.array(embeddings[:-1]), axis=0) # skip the last embedding
            avgEmbedding = np.mean (np.array(embeddings), axis=0)
            return avgEmbedding.tolist() # everything is list to save to pickle
        return embeddings[0]   # want to get the first result only
        
    #
    result = pd.read_csv (list_file, delimiter=",")
    # apply to each element
    # result['embedding'] = result['filename'].apply(lambda fn: getEmbedding (fn))
    # result['embedding'] = result['filename'].swifter.allow_dask_on_strings(enable=True).apply( 
    #     lambda fn: getEmbedding (fn))
    result['embedding'] = result['filename'].parallel_apply(getEmbedding)

    return result[['filename','speaker','embedding']]


def LoadEntrollmentData (savedEnrollModel="enrollmentModel_Vosk.pkl"):
    """
    This routine first loads the enrollment dataset (from
    enrollmentModel_Lite.pkl) if exists.  Otherwise, it creates one by
    processing audio files listed in a text file (enroll_list.csv).
    
    The enrollment dataset includes the speaker name and the audio
    embedding.
    """
    
    if os.path.isfile (savedEnrollModel):
        ## load enrollment data
        print ("Loading enrollment data from " + savedEnrollModel)
        [enroll_embs, speakers] = pickle.load (open (savedEnrollModel, 'rb'))
        enroll_embs = np.array (enroll_embs) # make it a 2D numpy array
        speakers = pd.Series (speakers, name="speaker") # make it named panda series
    else:
        print("Creating enrollment data....")
        enroll_result = get_embeddings_from_list_file (c.ENROLL_LIST_FILE)
        enroll_embs = enroll_result['embedding'].tolist() # a 2D list of floats
        speakers = enroll_result['speaker'].tolist()

        ## Save enrollment data
        print ("Saving enrollment data to " + savedEnrollModel)
        pickle.dump ([enroll_embs, speakers], open (savedEnrollModel, 'wb'))

        ## make embeddeding as 2D numpy array
        enroll_embs = np.array (enroll_embs) # make it a 2D numpy array
        speakers = pd.Series (speakers, name="speaker") # make it named panda series
    return (enroll_embs, speakers)




def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("testListFile", nargs='?',
                         help="in test set eg. all_test_list.csv, default is apollo_cfg/...")
    parser.add_argument ("resultFile", nargs='?',
                         help="out result file eg. all_results.csv, default is apollo_cfg/...")

    # Specify Example:
    parser.epilog=f'''Example:
        {sys.argv[0]} 
        {sys.argv[0]} all_test_list.csv all_results_vosk.csv
        '''

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


def Find50Thres (scores, correct):
    pIncorrects = []
    ratioIncorrects = []
    correct = np.asarray (correct)

    minScore = int(scores.min()) + 1
    allDecisionThres = range(minScore, 101)
    for decisionThres in allDecisionThres:
        total = len (scores[scores < decisionThres])
        numCorrectDecisions = sum (correct[scores < decisionThres] == 0.0)
        prob_correct_decision = numCorrectDecisions / total
        pIncorrects.append (prob_correct_decision)
        ratioIncorrects.append (f"{numCorrectDecisions}/{total}")

        if prob_correct_decision < 0.5:
            print (f"decisionThres = {decisionThres}")
            print (f"prob_correct_decision = {prob_correct_decision}")
            return decisionThres - 1 # -1 to make sure better than 0.5
        
    return 50

def ReScale (scores, fiftyPoint):
    """
    """

    print ("Rescale fiftyPoint = ", fiftyPoint)
    def lerp (x, x0, x1, y0, y1):
        return y0 + (x - x0) / (x1 - x0) * (y1 - y0)

    def scale (score):
        if score < fiftyPoint:
            return lerp (score, 0, fiftyPoint, 0, 50)
        return lerp (score, fiftyPoint, 100, 50, 100)

    return [scale(score) for score in scores]


def NearestNeighborClassifier (enroll_embs, speakers, test_embs, testListFile):
    """
    enroll_embs is a row-vector matrix.  Each row is a vector.  (n_samples, n_features)
    speakers is the lable that goes with enroll_embs
    """
    
    # 1.) Compute distance between every pair
    print("Comparing test samples against enroll samples using cosine distance")
    distances_mat = pd.DataFrame (
        cdist (test_embs, enroll_embs, # 10 rows (test) by 200 columns (enroll) 
               metric=c.COST_METRIC), columns=speakers) # name of each column

    # 2.) Create a score table from test dataset, add the distances matrices to it
    scores = pd.read_csv (testListFile, delimiter=",",
                          header=0, names=['test_file','test_speaker'])
    scores = pd.concat ([scores, distances_mat], axis=1) # append columnwize

    # 3.) Use the simplest nearest neighbor classifier
    uSpeakers = speakers.unique()
    scores['predicted_spkr'] = scores[uSpeakers].idxmin(axis=1) # minium of each row
    scores['minDist'] = scores[uSpeakers].min(axis=1)           # minium of each row
    scores['degScore'] = computeScore (scores['minDist'])
    scores['correct'] = (scores['predicted_spkr'] == scores['test_speaker'])*1. # needed by eval
    print ("degScore ", scores['degScore'])
    print ("predicted_spkr is ", scores['predicted_spkr']) # TC 2019-12-31 (Tue) --
    # print("Writing outputs to [{}]....".format(c.RESULT_FILE))

    # 4.) Save result
    result = pd.concat ([scores.test_speaker, scores.predicted_spkr,
                         scores.degScore, scores.correct], axis=1)
    return result

# @mem.cache                      # TBD
def SVMClassifier (enroll_embs, speakers, test_embs, testListFile, classifierFile, metaFile):
    """
    enroll_embs is a row-vector matrix.  Each row is a vector.  (n_samples, n_features)
    classifierFile: stores the classifier as a pickle file and can be used by sklearn-porter
    metaFile: stores [uSpeakers, fiftyPoint]
    """

    # Normalize vectors
    X_normed = normalize (enroll_embs)

    # Create SVM classifier
    print("Fitting the classifier to the training set")
    t0 = time()

    # param_grid = {'C': [1e2, 5e2, 1e3, 5e3, 1e4],
    #               'gamma': [0.0001, 0.0005, 0.001, 0.005, 0.01, 0.1], }
    # clf = GridSearchCV(
    #     SVC(kernel='rbf', class_weight='balanced', probability=True), param_grid
    # )

    param_grid = {'C': [1e2, 5e2, 1e3, 5e3, 1e4]}
    clf = GridSearchCV(
        SVC(kernel='linear', class_weight='balanced', gamma='auto',
            decision_function_shape='ovo', probability=True), param_grid
            # decision_function_shape='ovr', probability=True), param_grid
    )

    # get train label
    uSpeakers = speakers.unique().tolist() # for java to read simple pickled list
    n_classes = len (uSpeakers)
    name2int = dict (zip (uSpeakers, range(n_classes)))
    y_train = [name2int[name] for name in speakers]

    # get best estimator
    clf = clf.fit (X_normed, y_train)
    print("done Training in %0.3fs" % (time() - t0))
    print ("Cross validation result:", clf.cv_results_)
    clf = clf.best_estimator_
    print("Best estimator found by grid search:", clf)
    
    # prepare Test data
    X_test_normed = normalize (test_embs)
    
    # get test label
    testInfo = pd.read_csv (testListFile, delimiter=",",
                          header=0, names=['test_file','test_speaker'])
    y_test = [name2int[name] for name in testInfo.test_speaker]

    print("Predicting on the test set")
    t0 = time()
    y_pred = clf.predict (X_test_normed)
    print("done predicting in %0.3fs" % (time() - t0))

    print(classification_report(y_test, y_pred, target_names=uSpeakers))
    print(confusion_matrix(y_test, y_pred, labels=range(n_classes)))

    # 4.) Save result
    int2name = dict (zip (range(n_classes), uSpeakers))
    predicted_spkr = [int2name[idx] for idx in y_pred]
    test_speaker = [int2name[idx] for idx in y_test]
    correct = [int (x==y) for (x,y) in zip(test_speaker, predicted_spkr)]
    scores = clf.predict_proba(X_test_normed).max(axis=1) * 100

    fiftyPoint = Find50Thres (scores, correct)
    scores_scaled = ReScale (scores, fiftyPoint)
    
    result = pd.DataFrame ([test_speaker, predicted_spkr,
                            scores_scaled, correct]).transpose()
    result.columns = ['test_speaker', 'predicted_spkr', 'degScore', 'correct']

    # Save the classifier
    print ("saved classifier")
    # pickle.dump([uSpeakers, clf, fiftyPoint], open (classifierFile, 'wb'))
    pickle.dump ([uSpeakers, fiftyPoint], open (metaFile, 'wb'))
    skjoblib.dump (clf, classifierFile, compress=0)

    return result


def LoadClassifierFromFile (classifierFile, metaFile, test_embs, testListFile):
    """
    test_embs is a row-vector matrix.  Each row is a vector.  (n_samples, n_features)
    """

    # [uSpeakers, clf, fiftyPoint] =  pickle.load(open (classifierFile, 'rb'))
    clf = skjoblib.load (classifierFile)
    # print ("testing all zeros: ", clf.predict (np.zeros((1,128))))
    [uSpeakers, fiftyPoint] = pickle.load (open (metaFile, 'rb'))
    n_classes = len (uSpeakers)
    name2int = dict (zip (uSpeakers, range(n_classes)))
    
    # prepare Test data
    # X_test_normed = normalize (test_embs)
    
    # get test label
    testInfo = pd.read_csv (testListFile, delimiter=",",
                            header=0, names=['test_file','test_speaker'])
    y_test = [name2int[name] for name in testInfo.test_speaker]

    print("Predicting on the test set")
    t0 = time()
    y_pred = clf.predict (test_embs)
    print("done in %0.3fs" % (time() - t0))

    print(classification_report(y_test, y_pred, target_names=uSpeakers))
    print(confusion_matrix(y_test, y_pred, labels=range(n_classes)))

    # 4.) Save result
    int2name = dict (zip (range(n_classes), uSpeakers))
    predicted_spkr = [int2name[idx] for idx in y_pred]
    test_speaker = [int2name[idx] for idx in y_test]
    correct = [int (x==y) for (x,y) in zip(test_speaker, predicted_spkr)]
    scores = clf.predict_proba(test_embs).max(axis=1) * 100

    scores_scaled = ReScale (scores, fiftyPoint)
    # scores_scaled = scores.tolist()      # TBD to show before calibration
    
    result = pd.DataFrame ([test_speaker, predicted_spkr,
                            scores_scaled, correct]).transpose()
    result.columns = ['test_speaker', 'predicted_spkr', 'degScore', 'correct']
    return result


def GaussianNBClassifier (enroll_embs, speakers, test_embs, testListFile, classifierFile, metaFile):
    """
    enroll_embs is a row-vector matrix.  Each row is a vector.  (n_samples, n_features)
    """

    # Normalize vectors
    # X_normed = normalize (enroll_embs)
    X_normed = enroll_embs

    # Create SVM classifier
    print("Fitting the classifier to the training set")
    t0 = time()
    clf = GaussianNB()
    
    # get train label
    uSpeakers = speakers.unique().tolist() # for java to read simple pickled list
    n_classes = len (uSpeakers)
    name2int = dict (zip (uSpeakers, range(n_classes)))
    y_train = [name2int[name] for name in speakers]

    # get best estimator
    clf = clf.fit (X_normed, y_train)
    print("done in %0.3fs" % (time() - t0))
    # print("Best estimator found by grid search:")
    # print(clf.best_estimator_)

    # prepare Test data
    # X_test_normed = normalize (test_embs)
    X_test_normed = test_embs
    
    # get test label
    testInfo = pd.read_csv (testListFile, delimiter=",",
                          header=0, names=['test_file','test_speaker'])
    y_test = [name2int[name] for name in testInfo.test_speaker]

    print("Predicting on the test set")
    t0 = time()
    y_pred = clf.predict (X_test_normed)
    print("done in %0.3fs" % (time() - t0))

    print(classification_report(y_test, y_pred, target_names=uSpeakers))
    print(confusion_matrix(y_test, y_pred, labels=range(n_classes)))

    # 4.) Save result
    int2name = dict (zip (range(n_classes), uSpeakers))
    predicted_spkr = [int2name[idx] for idx in y_pred]
    test_speaker = [int2name[idx] for idx in y_test]
    correct = [int (x==y) for (x,y) in zip(test_speaker, predicted_spkr)]
    scores = clf.predict_proba(X_test_normed).max(axis=1) * 100

    fiftyPoint = Find50Thres (scores, correct)
    scores_scaled = ReScale (scores, fiftyPoint)

    result = pd.DataFrame ([test_speaker, predicted_spkr,
                            scores_scaled, correct]).transpose()
    result.columns = ['test_speaker', 'predicted_spkr', 'degScore', 'correct']

    # Save the classifier
    print ("saved classifier")
    # pickle.dump([uSpeakers, clf, fiftyPoint], open (classifierFile, 'wb'))
    pickle.dump ([uSpeakers, fiftyPoint], open (metaFile, 'wb'))
    skjoblib.dump (clf, classifierFile, compress=0)

    return result



if __name__ == '__main__':
    """This routine first loads the enrollment data.  And then extract
    embedding from the test dataset, specified by "c.TEST_LIST_FILE".
    """

    # Create a parser:
    args = parseCommandLine ()
    print ("testListFile = ", args.testListFile)
    print ("resultFile = ", args.resultFile)
    input ("press enter to continue")
    
    # 1.) Load enrollment data:
    savedEnrollModel="enrollmentModel_vosk.pkl"
    (enroll_embs, speakers) = LoadEntrollmentData (savedEnrollModel)

    # 2.) Extract embeddings
    print("Processing test samples....")
    if args.testListFile == None:
        testListFile = c.TEST_LIST_FILE
    else:
        testListFile = args.testListFile
    test_result = get_embeddings_from_list_file (testListFile)
    test_embs = np.array(test_result['embedding'].tolist())

    # 3.) Classify
    scores = SVMClassifier (enroll_embs, speakers, test_embs, testListFile,
                            "svmClassifier_vosk.pkl", "meta_vosk.pkl")
    scores = LoadClassifierFromFile ("svmClassifier_vosk.pkl", "meta_vosk.pkl",
                                     normalize(test_embs), testListFile)

    # scores = NearestNeighborClassifier (enroll_embs, speakers, test_embs, testListFile);
    
    # scores = GaussianNBClassifier (enroll_embs, speakers, test_embs, testListFile,
    #                                "gnbClassifier_vosk.pkl", "meta_vosk.pkl")
    # scores = LoadClassifierFromFile ("gnbClassifier_vosk.pkl", "meta_vosk.pkl",
    #                                  test_embs, testListFile)

    # 4.) Save the result tofile
    if args.resultFile == None:
        result_dir = os.path.dirname (c.RESULT_FILE)
        if not os.path.exists (result_dir):
            os.makedirs (result_dir)

        (dirName, fileName) = os.path.split(c.RESULT_FILE)
        resultFilePath = os.path.join (dirName, os.path.splitext (fileName)[0] + "_vosk.csv")
    else:
        resultFilePath = args.resultFile

    print("Writing outputs to " + resultFilePath)
    with open (resultFilePath, 'w') as f:
        scores.to_csv(f, index=False, float_format='%.7f')

