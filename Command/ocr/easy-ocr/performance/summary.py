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

# import matplotlib
# font = {'family' : 'normal',
#         'weight' : 'bold',
#         'size'   : 22}
# matplotlib.rc('font', **font)
# matplotlib.rcParams.update({'font.size': 22})
# rcParams.update({'font.size': 16})


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
    categories = list(failures.keys())

    for category in categories:
        print (f"creating {category} Failrue.")
        if len(failures[category]):
            ranSample = random.sample (failures[category], 1)[0]
            imgFile = ranSample['imgFile']
            if category == "ar":
                gTruth = get_display (ranSample['gTruth']) # to display arabic properly
                ocrPred = get_display (ranSample['ocrPred']) # ..
            else:
                gTruth = ranSample['gTruth']
                ocrPred = ranSample['ocrPred']
                
            imshow (imread (imgFile))
            title (f'Ground Truth = "{gTruth}"\n              OCR = "{ocrPred}"')
            axis ("on")
            savefig (f"failure_mode_{category}.png", transparent=False,
                     pad_inches=0, bbox_inches='tight')
            show()
            

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
    (failures, accuracies) = pickle.load (open ("performance.pkl", 'rb'))

    # Test Dataset:
    values = list (accuracies.values())
    values = ["%.2f (%d/%d)" % elm for elm in values]
    keys = list (accuracies.keys())
    df2 = pd.DataFrame(values).transpose()
    df2.columns = keys
    print ("Test Dataset")
    print (df2)

    #------------------------
    # some failure cases
    #------------------------
    GetFailureMode (failures)
    

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

