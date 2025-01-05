#!/usr/bin/env python3
"""
Stand-alone Easy OCR program.

2020-09-03
"""

#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import sys
import os
import argparse
import textwrap
from pathlib import PosixPath, Path
import easyocr
import json
import cv2
import numpy as np

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
g_pipeline = None


#-------------------------
# Private Implementations 
#-------------------------
class NpEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        else:
            return super(NpEncoder, self).default(obj)

def run (inImgPath: PosixPath,
         outTxtPath: PosixPath,
         outImgPath: PosixPath=None,
         outInfoPath: PosixPath=None) -> bool:
    """Run keras-ocr.

    Args:
      inImgPath:
        Location of the input image file.
      outTxtPath:
        Location of the output text file.
      outImgPath:
        Location of the output overlay image file.
      outInfoPath:
        Location of the debgging text file.

    Returns:
      True: successful.
      False: no text detected OR unsuccessful.  See error messages.
    """

    ## Get the pipeline ready, if needed:
    global g_pipeline           # global variable here.
    if g_pipeline == None:
        g_pipeline = easyocr.Reader(['en',      # English
                                     'ar'])      # Arabics
                                     # 'ch_tra']) # Traditional Chinese

    ## Process the image
    image = cv2.imread (str(inImgPath))
    result = g_pipeline.readtext (image)

    ## Extract high confident text and their bounding boxes
    min_conf = 0.45
    textList = []
    for textInfo in result:
        (bbox, text, conf) = textInfo
        # print (f"textInfo = {textInfo}", flush=True)
        # if conf > min_conf:
        if True:                # confidence is unreliable
            textList.append (text)
            # (tl, tr, br, bl) = bbox
            # tl = (int(tl[0]), int(tl[1]))
            # tr = (int(tr[0]), int(tr[1]))
            # br = (int(br[0]), int(br[1]))
            # bl = (int(bl[0]), int(bl[1]))
            # cv2.rectangle (image, tl, br, (0, 255, 0), 2)

            # draw a polygone instead
            pts = np.array(bbox, np.int32)
            pts = pts.reshape((-1, 1, 2))
            cv2.polylines (image, [pts], True, (0, 255, 0), 2)
              
        # # normalize polygon coordinates to be saved to predictions.
        # for idx in range(4):
        #     bbox[idx][0] /= width
        # for idx in range(4):
        #     bbox[idx][1] /= height

            
    ## Save predition info out:
    if outInfoPath:
        (height, width, _) = image.shape
        outInfoPath.write_text (json.dumps ({"width":width, "height":height, "pred":result},
                                            cls=NpEncoder)) # result = [(bbox, text, conf)..]
        print (f"wrote info data to {outInfoPath}", flush=True)
    
    ## save output text and bounding box:
    allText = "".join(text + " " for text in textList)
    if len(allText) == 0: # if no text was detected
        return False
    outTxtPath.write_text (allText + "\n")

    ## Plot the predictions [optional]
    if outImgPath:
        cv2.imwrite (str(outImgPath), image)
        print (f"wrote debug overlay data to {outImgPath}", flush=True)

    return True


def parse_args () -> argparse.Namespace:
    # Create a parser:
    description="""
    KerasOcr text localization and OCR.
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("inImgFile", help="input image file")
    parser.add_argument ("outTxtFile", help="output text file")
    parser.add_argument ("outImgFile", help="output image overlay file")
    parser.add_argument ("outInfoFile", help="output predition info file.")

    # Specify Example:
    parser.epilog='''Example:
        %s tests/images/ocr.png out.txt out.png pred.json
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
    args = parse_args()
        
    #---------------------------
    # run the program :
    #---------------------------
    run (Path(args.inImgFile), Path(args.outTxtFile),
         Path(args.outImgFile), Path(args.outInfoFile))

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)

