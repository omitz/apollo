#! /usr/bin/env python3
#
#
# TC 2021-02-08 (Mon) 


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
try:
    sys.path.index(os.path.abspath("../"))
except:
    sys.path.append (os.path.abspath("../"))
import vgg16Places365_main
from pathlib import Path
import random
import pickle

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
    parser.add_argument ("jsonFile", help="sentiment json file")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")

    # Specify Example:
    parser.epilog='''Example:
        %s tests/Appliances_5.json
        ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


def GetJpegFiles (dirName):
    # Returns a list of file names under a directory
    imageFiles = [os.path.join (dirName, f) for f in os.listdir (dirName) 
                  if f.endswith ("jpeg")]
    return list (sorted (imageFiles))   # listdir has arbitrary sorting order



def ConductExp (dataSetDir):
    """
    """

    failures = {}
    accuracies = {}

    # 0.) Load the class names
    # load the class label
    classes = list()
    with open ("categories_places365.txt") as class_file:
        for line in class_file:
            classes.append(line.strip().split(' ')[0][3:])
    classes = tuple(classes)

    
    # 1.) load the images
    imageCategories = [(d, d.split('_')) for d in os.listdir(dataSetDir)
                       if os.path.isdir( os.path.join (dataSetDir, d) )]
    

    dataset = {}
    for (d, (category, inoutdoor)) in imageCategories:
        dataset [category] = GetJpegFiles (os.path.join (dataSetDir, d))

    # 2.) Classify each image
    tmpOutFile = "/dev/shm/sceneClassResult.txt"
    totalCorrectCount = 0
    totalCount = 0
    failures = {"indoor":[], "outdoor":[]}
    correctIndoorCount = 0
    correctOutdoorCount = 0
    totalIndoor = 0
    totalOutdoor = 0
    for (d, (category, inoutdoor)) in imageCategories:
        failures [category] = []
        correctClassCount = 0
        for imgFile in dataset [category]:
            (classHier, topClassesIdxs) = vgg16Places365_main.run (Path (imgFile),
                                                                   Path (tmpOutFile))
            if classHier == None:
                print ("ERROR: process_file Failed!", flush=True)
                1/0
            if classHier == "indoor":
                totalIndoor += 1
            else:
                totalOutdoor += 1

            # test to see if within top 5 category
            topClasses = [classes [idx] for idx in topClassesIdxs]
            totalCount += 1
            if category in topClasses:
                correctClassCount += 1 
                totalCorrectCount += 1
            else:
                # collect failure mode
                failures [category].append ((imgFile, topClasses))

            if (classHier == inoutdoor):
                if (classHier == "indoor"):
                    correctIndoorCount += 1
                else:
                    correctOutdoorCount += 1
                
        accuracies [category] = ((correctClassCount / len (dataset [category]) * 100),
                                 correctClassCount, len (dataset [category]))

    # 3.) update indoor / outdoor
    accuracies ["indoor"] = ((correctIndoorCount / totalIndoor) * 100,
                             correctIndoorCount, totalIndoor)
    accuracies ["outdoor"] = ((correctOutdoorCount / totalOutdoor) * 100,
                              correctOutdoorCount, totalOutdoor)
        
    # 4.) compute total accuracy
    accuracies ["total"] = ((totalCorrectCount / totalCount) * 100, totalCorrectCount, totalCount)
    return (failures, accuracies)



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

