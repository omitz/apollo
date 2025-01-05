#! /usr/bin/env python3
#
# This program takes ocr meta data and overlay bounding boxes onto the
# OCR output.
# 


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import json
import numpy as np
import cv2

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
    parser.add_argument ("inImg", help="input image")
    parser.add_argument ("pred", help="predition file")
    parser.add_argument ("outImg", help="output image")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")

    # Specify Example:
    parser.epilog='''Example:
        %s tests/images/ocr.png pred.json overlay.png
        ''' % (sys.argv[0])

    # Parse the commandline:
    try:
        args = parser.parse_args()
    except:
        print ("\n")
        parser.print_help()
        sys.exit (1)
    return args


# From https://www.pyimagesearch.com/2020/09/14/getting-started-with-easyocr-for-optical-character-recognition/
def cleanup_text(text):
	# strip out non-ASCII text so we can draw the text on the image
	# using OpenCV
	return "".join([c if ord(c) < 128 else "" for c in text]).strip()

    
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
    print ("args.inImg = ", args.inImg)
    print ("args.pred = ", args.pred)
    print ("args.outImg = ", args.outImg)


    #---------------------------
    # run the program :
    #---------------------------

    # 1.) Load image
    image = cv2.imread (args.inImg)
    (width, height, _) = image.shape

    with open (args.pred, "r") as f:
        # 2.) Load predition
        jsonContent = f.read ()
        dataDict = json.loads (jsonContent)
        predictions = dataDict['pred']

        # 3.) Overlay
        for prediction in predictions:
            (bbox, text, conf) = prediction
            print (f"prediction = {prediction}", flush=True)
            # if conf > min_conf:
            if True:                # confidence is unreliable
                (tl, tr, br, bl) = bbox
                tl = (int(tl[0]), int(tl[1]))
                # tr = (int(tr[0]), int(tr[1]))
                # br = (int(br[0]), int(br[1]))
                # bl = (int(bl[0]), int(bl[1]))
                # cv2.rectangle (image, tl, br, (0, 255, 0), 2)

                # draw a polygone instead
                pts = np.array(bbox, np.int32)
                pts = pts.reshape((-1, 1, 2))
                cv2.polylines (image, [pts], True, (0, 255, 0), 2)

                # overlay text 
                text = cleanup_text(text) # (only ascii character)
                cv2.putText(image, text, (tl[0], tl[1] - 10),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 1)
                
    
        # 4.) Save image output
        cv2.imwrite (args.outImg, image)

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

