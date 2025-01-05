#!/usr/bin/env python3
#
# GPU does (<2GB) does not seem to work??
#

#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys 
import argparse
import textwrap
import numpy as np
import csv
from PIL import Image
from cv2 import resize
from pathlib import PosixPath, Path


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
g_model = None


#-------------------------
# Private Implementations 
#-------------------------

def run (inImgPath: PosixPath, outTxtPath: PosixPath=None):
    """Runs the scene classification program.
    
    Args:
      inImgPath: 
        The image file (can be any image file format).
      outTxtPath:
        The scene output text.  To have the output dump to screen, just
        set it to "/dev/stdout".

    Returns:
      classHier:
        The class hierarchy -- either "indoor" or "outdoor"
      topClassesIdxs: 
        The index to the top 5 classes
    """

    global g_model
    if g_model == None:
        # only need to load model once.
        from vgg16_places_365 import VGG16_Places365 # initialize tensorflow stuff 
        g_model = VGG16_Places365 (weights='places') 
        

    image = Image.open(inImgPath).convert('RGB') # gets rid of alpha channel
    image = np.array (image, dtype=np.uint8)
    sqWidth = min (image.shape[0:2])
    crop_img = image[0:sqWidth, 0:sqWidth] # the largest square,
                                           # without distortion
    image = resize (crop_img, (224, 224))
    image = np.expand_dims (image, 0)

    predictions_to_return = 5
    try:
        preds = g_model.predict(image)[0]
        top_preds = np.argsort(preds)[::-1][0:predictions_to_return]
    except Exception as e:
        print ("%s" % str(e))
        print (f"ERROR: {inImgPath} corrupted?", flush=True)
        return (None, [])

    # load the class label
    file_name = 'categories_places365.txt'
    classes = list()
    with open(file_name) as class_file:
        for line in class_file:
            classes.append(line.strip().split(' ')[0][3:])
    classes = tuple(classes)

    #
    # load the hierarchy lable and establish offset:
    #
    csvContent = [x for x in csv.reader (open ('scene_hierarchy.csv','r'))] 
    hierIdxOff = [idx for idx in range(0, 10) # within 10
                  if "airfield" in csvContent[idx][0]][0]
    classColumn = [csvContent[idx][0] for idx in range(len(csvContent))]
    indoorColumn = [csvContent[idx][1] for idx in range(len(csvContent))]
    outdoorNaturalColumn = [csvContent[idx][2] for idx in range(len(csvContent))]
    outdoorManmadeColumn = [csvContent[idx][3] for idx in range(len(csvContent))]

    
    #
    # show the top 5 predictions:
    #
    # print('--SCENE CATEGORIES:', flush=True)
    outTxtList = []
    topClassesIdxs = []
    indoorCount = 0
    outdoorCount = 0
    for i in range(0, 5):       # we just want top 5
        topClassIdx = top_preds[i]
        topClassesIdxs += [topClassIdx]
        className = classes[top_preds[i]]
        # print ("top %d" % i + ": " +  classes[topClassIdx], flush=True)
        outTxtList.append (classes[topClassIdx])
        
        topHierIdx = topClassIdx + hierIdxOff
        assert (className in classColumn[topHierIdx])
        indoor_flg = int (indoorColumn [topHierIdx])
        outdoorManmade_flg = int (outdoorManmadeColumn [topHierIdx])
        outdoorNatural_flg = int (outdoorNaturalColumn [topHierIdx])
        assert (indoor_flg + outdoorNatural_flg + outdoorManmade_flg)
        # if indoor_flg:
        #     print ("\tindoor", flush=True)
        # if outdoorManmade_flg:
        #     print ("\toutdoor man made", flush=True)
        # if outdoorNatural_flg:
        #     print ("\toutdoor natural", flush=True)
        if indoor_flg:
            indoorCount += 1
        if outdoorNatural_flg or outdoorManmade_flg:
            outdoorCount += 1

    #
    # output the hierarchy ("indoor" vs "outdoor")
    #
    # print('--SCENE HIERARCHY:', flush=True)
    if indoorCount > outdoorCount:
        # print ("Majority vote: indoor", flush=True)
        classHier = 'indoor'
        outTxt = 'indoor\n'
    else:
        # print ("Majority vote: outdoor", flush=True)
        classHier = 'outdoor'
        outTxt = 'outdoor\n'

    #
    # save the top 5 predictions:
    #
    if outTxtPath:
        outTxt += ("Top 5: " + outTxtList[0] + ''.join (["," + x for x in outTxtList[1:]]))
        outTxtPath.write_text (outTxt + "\n")

    # return the result so it can be populated to database
    return (classHier, topClassesIdxs)


def parseCommandLine ():
    """Parse commandline argurments.

    Returns:
      An argparse.Namespace object whose member variables correspond
      to commandline agruments. For example the "--debug" commandline
      option becomes member variable .debug.
    """

    description="""
    A scene classification program.
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("inImgFile", help="input image file")
    parser.add_argument ("outTxtFile", help="output text file")

    # Specify Example:
    parser.epilog='''Example:
    %s 6.jpg out.txt
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
    args = parseCommandLine ()

        
    #---------------------------
    # run the program :
    #---------------------------
    run (Path(args.inImgFile), Path(args.outTxtFile))

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)
