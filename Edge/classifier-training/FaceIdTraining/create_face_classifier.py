#! /usr/bin/env python3
#
# https://machinelearningmastery.com/how-to-develop-a-face-recognition-system-using-facenet-in-keras-and-an-svm-classifier/
#
# TC 2021-05-07 (Fri) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
from time import time
from sklearn.preprocessing import Normalizer
from sklearn.preprocessing import StandardScaler
from sklearn.preprocessing import LabelEncoder
from sklearn.svm import SVC
from sklearn.model_selection import GridSearchCV
from sklearn.metrics import accuracy_score
from libsvm.svmutil import *
import numpy as np
import joblib
import pickle

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------

def TrainSVM (trainX, trainy):
    param_grid = {'C': [1, 1e2, 5e2, 1e3, 5e3, 1e4]}
    clf = GridSearchCV(
        SVC(kernel='linear', class_weight='balanced', gamma='auto',
            decision_function_shape='ovo', probability=True), param_grid
            # decision_function_shape='ovr', probability=True), param_grid
    )

    # get best estimator
    t0 = time()
    clf = clf.fit (trainX, trainy)
    print("done Training in %0.3fs" % (time() - t0))
    print ("Cross validation result:", clf.cv_results_)
    clf = clf.best_estimator_
    print()
    print("Best estimator found by grid search:", clf)
    print()

    return clf



def TrainLibSVM (trainX, trainy, cVal):
    """
    trainX: array of embeddings (512-D)
    trainy: array of classifier id (integer)
    cVal: value of C
    """

    clf = svm_train (trainy, trainX,
                     '-q -c %d -t 0 -b 1' % cVal) # quiet, C=cVal, linear, and probability

    return clf


def WriteSvmProblem (filename, trainX, trainy, newFormat):
    """
    Save data to libsvm sparse matrix format
    """
    content = []
    for idx in range (len (trainy)):
        trainX_str = " ".join (["%d:%.17g" % (eidx+newFormat, elm)
                                for (eidx, elm) in enumerate (trainX[idx])])
        content.append (str (trainy[idx]) + " " + trainX_str)


    open (filename, 'w').writelines(["%s\n" % item for item in content])


def TransformTrainData (trainX, trainy):
    out_encoder = LabelEncoder()
    out_encoder.fit (trainy)    # conver face name to integer in sorted order
    newTrainy = out_encoder.transform(trainy)
    nClasses = len (list(set(newTrainy)))
    
    newTrainX = trainX          # no normalization
    return (newTrainX, newTrainy, nClasses, out_encoder)
    
def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("faceEmbeddingFile", help="The input face embedding pickle file")
    parser.add_argument ("classiferFile", help="The output classifier")
    parser.add_argument ("faceLabelFile", help="The output face label mapping file")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")

    # Specify Example:
    parser.epilog='''Example:
        %s faces-embeddings.pkl svm.pkl label
        ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    #------------------------------
    # parse command-line arguments:
    #------------------------------
    # Create a parser:
    args = parseCommandLine ()

    # Access the options:
    print ()
    print ("faceEmbeddingFile = ", args.faceEmbeddingFile)
    print ("classiferFile = ", args.classiferFile)
    print ("faceLabelFile = ", args.faceLabelFile)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------
    # trainX, trainy = pickle.load (open ("faces-embeddings.pkl", 'rb'))
    trainX, trainy = pickle.load (open (args.faceEmbeddingFile, 'rb'))
    trainX = np.asarray (trainX, trainX[0].dtype)
    ## load from data on edge device
    # trainy, trainX = svm_read_problem('scaffold/faceID.data', return_scipy=True)
    # trainy, trainX = svm_read_problem('data.txt', return_scipy=True)
    # trainX = np.asarray (trainX.todense())
    
    ## encode the input embedding
    # newTrainX = trainX          # no normalization
    # in_encoder = Normalizer (norm='l2') # normailze embedding
    # in_encoder.fit (trainX)
    # newTrainX = in_encoder.transform (trainX)
    # use 0-mean 1-std (whitening)
    # in_encoder = StandardScaler()
    # in_encoder.fit (trainX.T)
    # newTrainX = in_encoder.transform (trainX.T).T
    # newTrainX = PreWhiten (trainX) # std=1 mean=0 for each embedding

    ## encode the output label
    # out_encoder = LabelEncoder()
    # out_encoder.fit (trainy)    # conver face name to integer in sorted order
    # newTrainy = out_encoder.transform(trainy)
    # nClasses = len (list(set(newTrainy)))

    (newTrainX, newTrainy, nClasses, out_encoder) = TransformTrainData (trainX, trainy)
    
    # # test saving to libsvm data format
    # WriteSvmProblem ('data.txt', newTrainX, newTrainy, 1)
    # newTrainy, newTrainX = svm_read_problem('data.txt', return_scipy=True)
    # newTrainX = np.asarray (newTrainX.todense())
    
    # Mapping from id to name
    face_labels = out_encoder.inverse_transform (range(nClasses)) # face labels are always sorted 
    open (args.faceLabelFile, 'w').writelines("%s\n" % item for item in face_labels)
    print ("Wrote to ", args.faceLabelFile)

    # fit SVM model
    clf = TrainSVM (newTrainX, newTrainy)
    pickle.dump (clf, open (args.classiferFile, 'wb'))
    # joblib.dump (clf, "svm_v2.pkl", compress=0)
    print ("Wrote to ", args.classiferFile)

    # check performance 
    yhat_train = clf.predict (newTrainX)
    score_train = accuracy_score (newTrainy, yhat_train)
    print('Accuracy: train=%.3f ' % (score_train*100))

    p_label_idx = clf.predict (newTrainX[0].reshape(1,-1))
    p_vals = clf.predict_proba (newTrainX[0].reshape(1,-1))[0]
    maxPval_idx = np.argmax (p_vals)
    assert (p_label_idx == maxPval_idx)
    print ("Face is ", face_labels[maxPval_idx])
    print ("Prob is ", p_vals[maxPval_idx])
    
    
    # # fit libSVM model directly
    # clf2 = TrainLibSVM (newTrainX, newTrainy)
    # svm_save_model('libsvm_v2.model', clf2)
    # m1 = svm_load_model("libsvm_v2.model")
    # open ("libsvm_v2.label", 'w').writelines("%s\n" % item for item in face_labels)
    # # check performance
    # print ("testing direct libsvm method")
    # p_label, p_acc, p_val = svm_predict (newTrainy, newTrainX, m1, '-b 1') # probability
    

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

