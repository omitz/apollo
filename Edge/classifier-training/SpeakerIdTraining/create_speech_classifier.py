#! /usr/bin/env python3
#
# 
#
# TC 2021-05-20 (Thu) 


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
from time import time
from sklearn.preprocessing import LabelEncoder
from sklearn.preprocessing import normalize
from sklearn.preprocessing import Normalizer
from sklearn.metrics import accuracy_score
import numpy as np
import pickle 

from sklearn.svm import SVC
from sklearn.model_selection import GridSearchCV

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


def TransformTrainData (trainX, trainy):
    ## 2.) encode the input embedding
    # newTrainX = trainX          # no normalization??
    in_encoder = Normalizer (norm='l2') # normailze embedding, row-based (to match java side)
    in_encoder.fit (trainX)
    newTrainX = in_encoder.transform (trainX)

    ## 3.) encode the output label
    out_encoder = LabelEncoder()
    out_encoder.fit (trainy)    # conver face name to integer in sorted order
    newTrainy = out_encoder.transform(trainy)
    nClasses = len (list(set(newTrainy)))

    return (newTrainX, newTrainy, nClasses, out_encoder)

    
def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("speechEmbeddingFile", help="The input speech embedding pickle file")
    parser.add_argument ("classiferFile", help="The output classifier")
    parser.add_argument ("speechLabelFile", help="The output face label mapping file")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")

    # Specify Example:
    parser.epilog='''Example:
        %s speech-embeddings.pkl svm.pkl label
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
    print ("speechEmbeddingFile = ", args.speechEmbeddingFile)
    print ("classiferFile = ", args.classiferFile)
    print ("speechLabelFile = ", args.speechLabelFile)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------

    # 1.) Load enrollment data:
    trainX, trainy = pickle.load (open (args.speechEmbeddingFile, 'rb'))
    newTrainX, newTrainy, nClasses, out_encoder = TransformTrainData (trainX, trainy)
    
    # ## 2.) encode the input embedding
    # # newTrainX = trainX          # no normalization??
    # in_encoder = Normalizer (norm='l2') # normailze embedding, row-based (to match java side)
    # in_encoder.fit (trainX)
    # newTrainX = in_encoder.transform (trainX)

    # ## 3.) encode the output label
    # out_encoder = LabelEncoder()
    # out_encoder.fit (trainy)    # conver face name to integer in sorted order
    # newTrainy = out_encoder.transform(trainy)
    # nClasses = len (list(set(newTrainy)))

    # ## 4.) Mapping from id to name
    speech_labels = out_encoder.inverse_transform (range(nClasses)) # labels are always sorted 
    open (args.speechLabelFile, 'w').writelines("%s\n" % item for item in speech_labels)
    print ("Wrote to ", args.speechLabelFile)
    
    ## 5.) Fit SVM model
    clf = TrainSVM (newTrainX, newTrainy)
    pickle.dump (clf, open (args.classiferFile, 'wb'))
    # joblib.dump (clf, "svm_v2.pkl", compress=0)
    print ("Wrote to ", args.classiferFile)

    ## 6.) Check performance 
    yhat_train = clf.predict (newTrainX)
    score_train = accuracy_score (newTrainy, yhat_train)
    print('Accuracy: train=%.3f ' % (score_train*100))

    p_label_idx = clf.predict (newTrainX[0].reshape(1,-1))
    p_vals = clf.predict_proba (newTrainX[0].reshape(1,-1))[0]
    maxPval_idx = np.argmax (p_vals)
    assert (p_label_idx == maxPval_idx)
    print ("Speech is ", speech_labels[maxPval_idx])
    print ("Prob is ", p_vals[maxPval_idx])


    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

