#! /usr/bin/env python3
#
#
# TC 2021-02-22 (Mon) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import json
import csv
import sys, os
if (not os.path.abspath("../") in sys.path):
  sys.path.append (os.path.abspath("../"))
import vosk_main
from pathlib import Path
import random
import pickle
import pandas as pd
import io

import editdistance

from joblib import Memory
mem = Memory(cachedir='/tmp/joblib', verbose=1)
from pandarallel import pandarallel
pandarallel.initialize()

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
    parser.add_argument ("csvFile", help="csv file for test dataset")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")

    # Specify Example:
    parser.epilog='''Example:
        %s tests/testDataSet/samples.csv
        ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


def vosk_main_wrapper (audioFile):

  textOut = io.StringIO()
  succeed = vosk_main.run (audioFile, textOut)
  assert (succeed)
  text = textOut.getvalue()
  textOut.close()
  return text
    

@mem.cache
def getPreditions (audioFiles):
  return audioFiles.parallel_apply (vosk_main_wrapper)


def ConductExp (dataSetDir):
    """
    """


    # 1.) Load samples
    metadata = pd.read_csv (Path (dataSetDir) / 'samples.csv')
    audioFiles = [Path (dataSetDir) / filename for filename in metadata.filename]
    gTruths = metadata.text.to_list() 

    # 2.) run speech to text for each audio
    audioFiles = pd.Series (audioFiles)
    vosk_main_wrapper (audioFiles[0]) # first time load the model
    predictions = getPreditions (audioFiles).values

    # 3.) compute the WER, accuracy, and the failure modes
    nWords = 0
    eDist = 0
    tranTbl = "".maketrans('', '', '.,\n') # remove punctuations
    maxDist = 0
    for (idx, (pred, gTruth)) in enumerate (zip (predictions, gTruths)):
      pred = pred.translate (tranTbl) # remove punctuations
      predWords = pred.lower().split(" ")
      gTruthWords = gTruth.lower().split (" ")

      dist = editdistance.eval(gTruthWords, predWords)
      eDist += dist
      nWords += len (gTruthWords)

      # keep track of the worst result
      if dist > maxDist:
        maxDist = dist
        failure = {"gTruth": gTruthWords, "prediction":predWords, "eDist":maxDist}

    WER = eDist / nWords
    accuracy = {'WEB': WER, "nWords": nWords, "nAudios": len(audioFiles)}
    
    
    return (failure, accuracy)



#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    #------------------------------
    # parse command-line arguments:
    #------------------------------
    # Create a parser:
    # args = parseCommandLine ()

    # Access the options:
    # print ("args.jsonFile = ", args.jsonFile)


    #---------------------------
    # run the program :
    #---------------------------
    (failures, accuracies) = ConductExp ("../tests/testDataset")

    # save for plotting/summarizing
    print ("Saving to performance.pkl")
    pickle.dump ([failures, accuracies],
                 open ("performance.pkl", 'wb'))
    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

