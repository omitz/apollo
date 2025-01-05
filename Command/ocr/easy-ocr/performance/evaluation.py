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
import easyOcr_main
# import tesseract_main
from pathlib import Path
import random
import pickle
import cv2

from PIL import Image, ImageDraw, ImageFont
import arabic_reshaper
import io


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


def GetImgFiles (dirName, ext):
    # Returns a list of file names under a directory
    imageFiles = [os.path.join (dirName, f) for f in os.listdir (dirName) 
                  if f.endswith (ext)]
    return list (sorted (imageFiles))   # listdir has arbitrary sorting order


def VisualCompareWord (gWord, pWord):
    
    font = ImageFont.truetype (
        "Arial.ttf", # Arial since it's a well known font that supports Arabic Unicode
        40, encoding="unic")

    pWordImg = Image.new('RGBA', (800, 60), (255,255,255))
    pWordImg_draw = ImageDraw.Draw(pWordImg)
    pWordImg_draw.text((10,10), pWord, fill=(0,0,0), font=font)

    gWordImg = Image.new('RGBA', (800, 60), (255,255,255))
    gWordImg_draw = ImageDraw.Draw(gWordImg)
    gWordImg_draw.text((10,10), gWord, fill=(0,0,0), font=font)

    s = io.BytesIO()
    gWordImg.save(s, 'png')
    gWordImg.save('gWord.png')
    gWordImg_data = s.getvalue()

    s = io.BytesIO()
    pWordImg.save(s, 'png')
    pWordImg.save('pWord.png')
    pWordImg_data = s.getvalue()

    if pWordImg_data == gWordImg_data:
        return True

    return False

    
def VisualCompare (gWord, pWords_str):
    pWords = pWords_str.split (" ")
    for pWord in pWords:
        if VisualCompareWord (gWord, pWord):
            return True
    return False
    
    
def ConductExp (dataSetDir):
    """
    """

    failures = {}
    accuracies = {}

    # 1.) load the images
    langCategories = [d for d in os.listdir(dataSetDir)
                      if os.path.isdir( os.path.join (dataSetDir, d) )]
    # langCategories = ["ar"]     # TBF
    # langCategories = ["es"]     # TBF

    dataset = {}
    for lang in langCategories:
        dataset [lang] = GetImgFiles (os.path.join (dataSetDir, lang), "jpg")

    # 2.) ocr each image
    totalCorrectCount = 0
    totalCount = 0
    failures = {}
    totalClassCount = {}
    to_tess_lang = {"en":"eng", "ru":"rus", "es":"spa", "ar":"ara", "fr":"fra"}
    uniq_failure_entries = {}
    for lang in langCategories:
        failures [lang] = []
        correctClassCount = 0
        totalClassCount [lang] = 0
        for imgFile in dataset [lang]:
            # get ground-truth from file name
            gTruth = os.path.basename (imgFile).split ("_")[0]
            gTruth = gTruth.lower() # TC 2021-02-23 (Tue) --
            print ("easyOcr_main.run...")
            tmpOutFile = "/dev/shm/" + os.path.basename (imgFile).replace(".jpg", ".txt")
            overlayFile = "overaly_" + os.path.basename (imgFile).replace(".jpg", ".png")

            # succeed = easyOcr_main.run (Path (imgFile), Path (tmpOutFile), "delme.png")
            print (f"set language to {to_tess_lang[lang]}")
            succeed = easyOcr_main.run (Path (imgFile), Path (tmpOutFile), overlayFile, None)
            assert (succeed)


            ocrPred = open (tmpOutFile, "r").read ().strip()
            ocrPred = ocrPred.lower() # TC 2021-02-23 (Tue) --

            # print ("* original ocrPred = ", ocrPred)
            # print ("* original file = ", Path (imgFile))
            if lang == "ar":
                # 1.) ocrPred nees reshape
                # 2.) ground-truth is inputed "backward" to look like pictured and needs reversed
                word_list =[]
                for word in gTruth.split(" "):
                    rev_word = word[::-1]  # slice backwards
                    reshaped_word = arabic_reshaper.reshape(rev_word)
                    word_list = [reshaped_word] + word_list # reverse word order as well
                gTruth = " ".join(word_list)
                    
                word_list =[]
                for word in ocrPred.split(" "):
                    reshaped_word = arabic_reshaper.reshape(word)
                    word_list.append (reshaped_word)
                ocrPred = " ".join(word_list)

                
            # print ("* gTruth  = ", gTruth)
            # print ("* ocrPred = ", ocrPred)
            
            # # match by complete sentence:
            # totalCount += 1
            # totalClassCount [lang] += 1
            # if (gTruth == ocrPred): 
            #     correctClassCount += 1 
            #     totalCorrectCount += 1

            # match by individual word:
            gWords = gTruth.split(' ')
            print ("len (gWords) = ", len (gWords))
            totalCount += len (gWords)
            totalClassCount [lang] += len (gWords)
            for word in gWords:
                if (word in ocrPred) or (VisualCompare (word, ocrPred)):
                    # print ("correct")
                    correctClassCount += 1 
                    totalCorrectCount += 1
                else:
                    # collect failure modes
                    print ("***failure")
                    print ("* word  = ", word)
                    print ("* gTruth  = ", gTruth)
                    print ("* ocrPred = ", ocrPred)
                    image = cv2.imread(imgFile)
                    rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
                    # tess_lang = tesseract_main.determine_lang_arg (rgb) # for debugging info
                    failure_entry = {"imgFile":imgFile,
                                     "gTruth":gTruth,
                                     "ocrPred":ocrPred}
                                     # "tess_lang":tess_lang}
                    if (not str(failure_entry) in uniq_failure_entries):
                        # don't collect multiple failed case in the same image
                        uniq_failure_entries[str(failure_entry)] = 1
                        failures [lang].append (failure_entry)
                        print (f"* Append failures {failures}")
                    
        accuracies [lang] = ((correctClassCount / totalClassCount [lang]) * 100,
                                 correctClassCount, totalClassCount [lang])
            
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

