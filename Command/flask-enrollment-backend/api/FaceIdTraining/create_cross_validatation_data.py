#! /usr/bin/env python3
#
# This program takes in a directory containing the face data.  It then
# does a k fold stratified cross validation of the entire data set and
# save the reslut to files.
#
# To plot the results, use show_cross_validation_result.py
#
# TC 2021-09-09 (Thu) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
from sklearn.model_selection import StratifiedKFold
import numpy as np
from sklearn.metrics import accuracy_score
import pickle

import create_face_dataset
import create_face_embeddings_v2
import create_face_classifier

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------
def run_from_ipython():
    try:
        __IPYTHON__
        return True
    except NameError:
        return False

def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("vipTopDir", help="the vip top directory")
    parser.add_argument ("k", help="k used in cross validation ")
    parser.add_argument ("foldDir", help="directory to save the fold data ")
    parser.add_argument ("maxClassExamples", nargs='?',
                         help="Optional max examples per class limit")

    # Specify Example:
    parser.epilog=f'''Example:
        {sys.argv[0]} vips_with_profile/ 3
        {sys.argv[0]} vips_with_profile/ 2 20
        '''

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        if run_from_ipython():
            1/0
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
    print ("vipTopDir = ", args.vipTopDir)
    print ("args.k = ", args.k)
    print ("args.foldDir = ", args.foldDir)
    print ("maxClassExamples = ", args.maxClassExamples)
    input ("press enter to continue")
    
    #---------------------------
    # run the program :
    #---------------------------
    ## 1.) pre-compute embeddings for the entire dataset
    create_face_dataset.init ()
    if (args.maxClassExamples != None):
        maxExamples = int (args.maxClassExamples)
        print (f"max data limited to {maxExamples} per class")
        [dataX, datay] = create_face_dataset.LoadFaceDataset (
            args.vipTopDir, maxExamples) 
    else:
        print ("use all data")
        [dataX, datay] = create_face_dataset.LoadFaceDataset (args.vipTopDir)

    # transform training
    create_face_embeddings_v2.init ()
    dataX = create_face_embeddings_v2.GetEmbeddings (dataX)
    newTrainX, newTrainy, nClasses, out_encoder = (
        create_face_classifier.TransformTrainData (dataX, datay))
    
    target_names = out_encoder.inverse_transform (range(nClasses)) # labels are always sorted 
    target_names = target_names.tolist()
    labels = range (nClasses)
    
    ## 2.) Create stratified kfold cross validation
    k = int (args.k)
    skf = StratifiedKFold (n_splits=k, shuffle=True)

    for idx, (train_index, test_index) in enumerate (skf.split (newTrainX, newTrainy)):
        trainX = newTrainX [train_index]
        trainy = newTrainy [train_index]
        clf = create_face_classifier.TrainSVM (trainX, trainy)
        yhat_train = clf.predict (trainX)
        score_train = accuracy_score (trainy, yhat_train)
        print('Accuracy: train=%.3f ' % (score_train*100))

        X_test = newTrainX [test_index]
        y_test = newTrainy [test_index]
        y_pred = clf.predict (X_test)
        scores = clf.predict_proba(X_test).max(axis=1) * 100
        foldFile = os.path.join (args.foldDir, f"{k}-fold{idx}.pkl")
        pickle.dump ([y_test, y_pred, scores, labels, target_names], open (foldFile, 'wb'))
        print (f"wrote {foldFile}")
        
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)


