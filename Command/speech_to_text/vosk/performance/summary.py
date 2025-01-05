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
from bidi.algorithm import get_display 
from PIL import Image, ImageDraw, ImageFont
from pylab import *
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
    (failure, accuracyInfo) = pickle.load (open ("performance.pkl", 'rb'))

    accuracy = 1.0 - accuracyInfo['WEB']
    nWords = accuracyInfo["nWords"]
    nAudios = accuracyInfo["nAudios"]
    print (f"accuracyInfo = {accuracyInfo}")
    
    print (f"Accuracy = {accuracy}")
    print (f"Num words = {nWords}")
    print (f"Num audios = {nAudios}")
    print (f"Avg num words per audio = {nWords/nAudios}")
    
    gTruth = " ".join(failure['gTruth'])
    prediction = " ".join(failure['prediction'])
    eDist = failure['eDist']
    print ("Worst failure Mode:")
    print (f"gTruth = \"{gTruth}\"")
    print (f"predition = \"{prediction}\"")
    print (f"edit distance = \"{eDist}\"")
    
    
    #------------------------
    # some failure cases
    #------------------------
    

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

