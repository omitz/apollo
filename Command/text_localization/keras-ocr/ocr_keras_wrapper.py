#!/usr/bin/env python3
#
# import matplotlib.pyplot as plt
# import keras_ocr
# pipeline = keras_ocr.pipeline.Pipeline()
# images = [ keras_ocr.tools.read('Army_Reserves_Recruitment_Banner_MOD_45156284.jpg') ]
# prediction_groups = pipeline.recognize(images)
# # Print the predictions -- text and bounding box
# print(prediction_groups) 
# # Plot the predictions [optional]
# fig, ax = plt.subplots(nrows=1, figsize=(20, 20))
# keras_ocr.tools.drawAnnotations(image=images[0], predictions=prediction_groups[0], ax=ax)
# fig.savefig ("out.png")


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import argparse, sys
import textwrap
import os, sys                                 # exit, argv
import keras_ocr
import matplotlib.pyplot as plt


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
g_pipeline = None

#-------------------------
# Private Implementations 
#-------------------------
def RunOcr (inImgPath, outTxtFile, outImgFile):
    """
    inImgPath - file location of the input image file
    outTxtFile - file location of the output text file
    """

    ## Get the pipeline ready, if needed:
    global g_pipeline           # global variable here.
    if g_pipeline == None:
        g_pipeline = keras_ocr.pipeline.Pipeline()

    ## set image(s) to process:
    images = [ keras_ocr.tools.read(inImgPath) ]

    ## set image(s) to process:
    prediction_groups = g_pipeline.recognize (images)

    ## convert prediction group to text
    text = [prediction_groups[0][idx][0] for idx in range(len (prediction_groups[0]))]

    ## save output text and bounding box:
    with open (outTxtFile, "w") as outfile:
        outfile.write (str(text))
        
    ## Plot the predictions [optional]
    fig, ax = plt.subplots(nrows=1, figsize=(20, 20))
    keras_ocr.tools.drawAnnotations(image=images[0], predictions=prediction_groups[0], ax=ax)
    fig.savefig (outImgFile)



def parse_args ():
    # Create a parser:
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("inImgFile", help="input image file")
    parser.add_argument ("outTxtFile", help="output text file")
    parser.add_argument ("outImgFile", help="output image overlay file")

    # Specify Example:
    parser.epilog='''Example:
        %s Army_Reserves_Recruitment_Banner_MOD_45156284.jpg out.txt out.png
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
    RunOcr (args.inImgFile, args.outTxtFile, args.outImgFile)

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)
