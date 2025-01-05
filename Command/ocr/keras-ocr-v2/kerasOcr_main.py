#!/usr/bin/env python3
"""
Stand-alone Kera OCR program.

2020-07-14
"""

#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import sys
import os
import argparse
import textwrap
from pathlib import PosixPath, Path
import keras_ocr
import matplotlib.pyplot as plt
import json
import numpy as np

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
g_pipeline = None

#-------------------------
# Private Implementations 
#-------------------------

def ConvertPreditionToJson (width, height, predictions):
    """
     preditions is an array of preditions.
         word= prediction[i][0]
     corners = prediction[i][1]
                [[ 24.  36.]
                 [104.  36.]
                 [104.  73.]
                 [ 24.  73.]]
     We want to convert to array of triplets:
       [(bbox, text, conf)]
    """

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

    metaOut = []
    for prediction in predictions:
        text = prediction[0]
        bbox = prediction[1]
        conf = 1.0        # fake confidence
        metaOut.append ((bbox, text, conf))
    return json.dumps ({"width":width, "height":height, "pred":metaOut}, cls=NpEncoder)
    

def run (inImgPath: PosixPath, outTxtPath: PosixPath,
         outImgPath: PosixPath=None, outInfoPath: PosixPath=None) -> bool:
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
      False: unsuccessful.  See error messages.
    """

    ## Get the pipeline ready, if needed:
    global g_pipeline           # global variable here.
    if g_pipeline == None:
        g_pipeline = keras_ocr.pipeline.Pipeline()

    ## set image(s) to process:
    images = [ keras_ocr.tools.read(str(inImgPath)) ]
    prediction_groups = g_pipeline.recognize (images)

    ## Save predition info out:
    if outInfoPath:
        (height, width, _) = images[0].shape
        outInfoPath.write_text (ConvertPreditionToJson (width, height, prediction_groups[0]))

    ## convert prediction group to text
    wordList = [prediction_groups[0][idx][0] for idx in range(len (prediction_groups[0]))]
    text = "".join(word + " " for word in wordList)

    ## save output text and bounding box:
    outTxtPath.write_text (text + "\n")
        
    ## Plot the predictions [optional]
    if outImgPath:
        fig, ax = plt.subplots(nrows=1, figsize=(20, 20))
        keras_ocr.tools.drawAnnotations(image=images[0],
                                        predictions=prediction_groups[0], ax=ax)
        fig.savefig (outImgPath)

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
