#!/usr/bin/env python3
# 
# This program takes the classifier model and a unknown test dataset.
# It save the data for subsequent showing.
#
# TC 2021-09-10 (Fri) --



#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import pickle 
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import confusion_matrix
from sklearn.metrics import classification_report

# import create_dataset_lists
# import create_speech_embeddings
# import create_speech_classifier
import create_face_dataset
import create_face_embeddings_v2
import create_face_classifier


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------



def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("dataTopDir", help="the unknown dataset top directory")
    parser.add_argument ("svm", help="The svm classifer")
    parser.add_argument ("out", help="The output score data ")

    # Specify Example:
    parser.epilog='''Example:
        %s unknown_dataset svm.pkl unknown_scores.pkl
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
    print ("args.dataTopDir = ", args.dataTopDir)
    print ("args.svm = ", args.svm)
    print ("args.out = ", args.out)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------
    # Load the classifier
    clf = pickle.load (open (args.svm, 'rb'))

    # create the unknow dataset
    create_face_dataset.init ()
    [dataX, datay] = create_face_dataset.LoadFaceDataset (args.dataTopDir)
    create_face_embeddings_v2.init ()
    dataX = create_face_embeddings_v2.GetEmbeddings (dataX)
    X_test, y_test, nClasses, out_encoder = (
        create_face_classifier.TransformTrainData (dataX, datay))

    # evaluate the unknown dataset 
    scores = clf.predict_proba(X_test).max(axis=1) * 100
    print (f"scores = {scores}")
    print (f"max score = {max(scores)}")
    
    # save the scores
    pickle.dump (scores, open (args.out, 'wb'))
    
    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)


