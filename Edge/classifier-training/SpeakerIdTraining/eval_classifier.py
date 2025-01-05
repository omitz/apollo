#!/usr/bin/env python3
# 
# This program takes the classifier model and the test dataset.
# It returns a confusion matrix.
#
# TC 2021-09-08 (Wed) --



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
    parser.add_argument ("svm", help="The svm classifer")
    parser.add_argument ("val", help="The validation data set")
    parser.add_argument ("label", help="The output lable mapping")
    parser.add_argument ("out", help="The output data for confusion matrix ")

    # Specify Example:
    parser.epilog='''Example:
        %s svm.pkl test-embeddings.pkl label confusion.pkl
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
    print ("args.svm = ", args.svm)
    print ("args.val = ", args.val)
    print ("label", args.label)
    print ("args.out = ", args.out)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------
    # Load the classifier
    clf = pickle.load (open (args.svm, 'rb'))

    # map label to integer ID (starting from 0)
    uSpeakers = [line.strip() for line in open (args.label, "r").readlines()]
    out_encoder = LabelEncoder()
    out_encoder.fit (uSpeakers)    # conver face name to integer in sorted order

    # Load the validation dataset
    [X_test, y_test] = pickle.load (open (args.val, 'rb'))
    
    # transform validation output to integer ID
    y_test = out_encoder.transform (y_test)

    # evaluate performance
    y_pred = clf.predict (X_test)
    print (clf.score (X_test, y_test))
    
    # create a confusion matrix
    n_classes = len (uSpeakers)
    labels = range (n_classes)
    target_names = uSpeakers
    pickle.dump ([y_test, y_pred, labels, target_names], open (args.out, 'wb'))

    # to create confusion matrix and report
    cnfsnMtrx = confusion_matrix (y_test, y_pred, labels)
    print (confusion_matrix (y_test, y_pred, labels))
    print (classification_report (y_test, y_pred, target_names=target_names))
    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)


