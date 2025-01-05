#! /usr/bin/env python3
#
# 
#
# TC 2021-02-04 (Thu) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import pickle
import pandas as pd
import random

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------

pd.options.display.float_format = "{:,.2f}".format


def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("pklFile", help="output of performance_evaluation")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")

    # Specify Example:
    parser.epilog='''Example:
        %s performance.pkl
        ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


def GetFailureMode (failures):
    print ("Positive Failrue:")
    if len(failures['positive']):
        ranSample = random.sample (failures['positive'], 1)
        print (ranSample)

    print ("Negative Failrue:")
    if len(failures['negative']):
        ranSample = random.sample (failures['negative'], 1)
        print (ranSample)

    print ("Neural Failrue:")
    if len(failures['neutral']):
        ranSample = random.sample (failures['neutral'], 1)
        print (ranSample)
    

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
    print ("args.pklFile = ", args.pklFile)


    #---------------------------
    # run the program :
    #---------------------------
    (amz_failures, amz_accuracies,
     tweet_failures, tweet_accuracies) = pickle.load (
         open ("performance.pkl", 'rb'))

    # Amazon Dataset:
    values = list (amz_accuracies.values())
    values = ["%.2f (%d/%d)" % elm for elm in values]
    keys = list (amz_accuracies.keys())
    df2 = pd.DataFrame(values).transpose()
    df2.columns = keys
    print ("Amazon Dataset")
    print (df2)
    
    # Tweet Dataset:
    values = list (tweet_accuracies.values())
    values = ["%.2f (%d/%d)" % elm for elm in values]
    keys = list (tweet_accuracies.keys())
    df2 = pd.DataFrame(values).transpose()
    df2.columns = keys
    print ("Tweet Dataset")
    print (df2)

    #------------------------
    # some failure cases
    #------------------------
    print ("Amazon Dataset")
    GetFailureMode (amz_failures)

    print ("Tweet Dataset")
    GetFailureMode (tweet_failures)
    

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

