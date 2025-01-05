#! /usr/bin/env python3
#
# A program to verify that the input image contains at least one image
# and is > 160x160 pixels.
#
# TC 2021-09-20 (Mon) --

#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
from PIL import Image
import json
from numpy import asarray
from mtcnn.mtcnn import MTCNN


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------


#-------------------------
# Private Implementations 
#-------------------------


def verify_face (filename, required_size=(160, 160)):
    """Verify a Face

    filename: the absolute path to image file.

    return: ((True/ False), "error":xxx, "face-xy:xxx, "face-size":xxx, "image-size":xxx} ) 
    True if a dominiant face was found and >= required_size
    """
    # load image from file and detect faces in the image
    image = Image.open(filename)
    image = image.convert('RGB')
    pixels = asarray(image)
    detector = MTCNN()
    results = detector.detect_faces(pixels)

    ## Pick the max confident face:
    try:
        bestResult = max (results, key=lambda e: e['confidence'])
        x1, y1, width, height = bestResult['box']
    except:
        return (False, {"error": "could not detect face"})
    
    # check error condition
    print (f"({x1},{y1}) width = {width}, height = {height}")
    if (x1 < 0) or (y1 < 0) or (width < required_size[0]) or (height < required_size[1]):
        return (False, {"error": f"face at {x1}x{y1} with size {width}x{height} is too small," +
                        f" must be > {required_size}",
                        "face-xy":f"{x1}x{y1}",
                        "face-size":f"{width}x{height}",
                        "image-size":f"{image.width}x{image.height}"})

    return (True, {"error":"",
                   "face-xy":f"{x1}x{y1}",
                   "face-size":f"{width}x{height}",
                   "image-size":f"{image.width}x{image.height}"})
        


def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("inputFile", help="The input image file")
    parser.add_argument ("jsonOutFile", nargs='?', help="optional json output file")

    # Specify Example:
    parser.epilog='''Example:
        %s file.jpg
        %s file.png json.out
        ''' % (sys.argv[0], sys.argv[0])

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
    print ("args.inputFile = ", args.inputFile)
    print ("jsonOutFile = ", args.jsonOutFile)
    input ("press enter to continue")

    #---------------------------
    # run the program :
    #---------------------------
    (valid, meta) = verify_face (args.inputFile)
    if args.jsonOutFile:
        encoded = json.dumps(meta)
        with open(args.jsonOutFile, 'w') as outFile:
            outFile.write (encoded)
        # with open(args.jsonOutFile, 'r') as outFile:
        #     encoded = outFile.read ()
        #     obj = json.loads(encoded)

    if valid:
        sys.exit (os.EX_OK)  
    
    sys.exit (os.EX_SOFTWARE)   # internal software error


    # #---------------------------
    # # program termination:
    # #---------------------------
    # print ("Program Terminated Properly\n", flush=True)

