#! /usr/bin/env python3
#
# modified from https://machinelearningmastery.com/how-to-develop-a-face-recognition-system-using-facenet-in-keras-and-an-svm-classifier/
#
# TC 2021-05-07 (Fri) --


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
from os import listdir
from os.path import isdir
from PIL import Image
from matplotlib import pyplot
# from numpy import savez_compressed
import random
from numpy import asarray
from mtcnn.mtcnn import MTCNN

# parallel job
import pandas as pd
from pandarallel import pandarallel

# import skimage.io
import pickle
from joblib import Memory

# turn off tensorflow warning
import tensorflow as tf
import logging

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
mem = Memory(cachedir='/tmp/joblib', verbose=1)

#-------------------------
# Private Implementations 
#-------------------------
@mem.cache
def extract_face (fn_metaId, required_size=(160, 160)):
    """Extract Face

    filename: the absolute path to image file.

    Extract a single face (highest confident) from a given photograph
    """
    (filename, metaId) = fn_metaId
    # load image from file and detect faces in the image
    image = Image.open(filename)
    image = image.convert('RGB')
    pixels = asarray(image)
    # pixels = skimage.io.imread (filename)
    detector = MTCNN()
    results = detector.detect_faces(pixels)


    ## Pick the max confident face:
    bestResult = max (results, key=lambda e: e['confidence'])
    x1, y1, width, height = bestResult['box']
    # bug fix
    # x1, y1 = abs(x1), abs(y1)
    print (f"bestResult['box'] = {bestResult['box']}, filename={filename}")
    print (f"x1={x1}, y1={y1}")
    assert (((x1 >= 0) and (y1 >= 0))), f"No face detected filename={filename}, bestResult={bestResult}"
    x2, y2 = x1 + width, y1 + height

    # extract the face and resize to model size
    face = pixels[y1:y2, x1:x2]
    image = Image.fromarray(face)
    # image = image.resize (required_size)
    image = image.resize (required_size, Image.BILINEAR) # match the phone quality
    face_array = asarray(image)
    return face_array


def GetFileMetaInfo (fileName):
    """
    Return a unique string about the file.
    """

    ## use hash
    ## use file size and date
    fstat = os.stat (fileName)
    return f"{fstat.st_mtime}{fstat.st_size}"


def load_faces_from_dir (directory):
    """Load Faces

    Load images and extract faces for all images in a directory.  
    Skip profile.jpg, if exits

    directory
    ├── 0001_01.jpg
    ├── 0010_01.jpg
    └── profile.jpg

    Return: A list of face arrays
    """
    # faces = list()
    # for filename in listdir(directory):
    #     path = directory + filename
    #     face = extract_face(path)
    #     faces.append(face)        
    # return faces

    filenames = [os.path.join (directory, filename)
                  for filename in os.listdir (directory)
                  if not filename.endswith("profile.jpg")]
    imageFiles = pd.Series ([(os.path.realpath(filename), GetFileMetaInfo (filename))
                             for filename in filenames])
    faces = imageFiles.parallel_apply (extract_face).to_list()
    return faces


def LoadFaceDataset (parentDir, maxSamples=None):
    """Load entire face dataset of multiple people

    Load a dataset that contains one subdir for each class that in turn contains images

    parentDir
    ├── Aishwarya_Rai_Bachchan
    │   ├── 0001_01.jpg
    │   ├── ...
    │   └── 0010_01.jpg
    ├── Ewan_McGregor
    │   ├── 0006_01.jpg
    │   ├── ...
    │   ├── 0116_01.jpg

    Return: a list of 2 arrays [X, y] where y[i] is the name of the
    face and X[i] is the face image
    """
    X, y = list(), list()
    # enumerate folders, on per class
    for subdir in sorted(listdir(parentDir)): # make it preditable and consistant order
        path = os.path.join (parentDir, subdir)
        # skip any files that might be in the dir
        if not isdir(path):
            continue

        # load all faces in the subparentDir, up to maxSamples
        faces = load_faces_from_dir(path)
        if (maxSamples != None) and (len (faces) > maxSamples):
            faces = random.sample (faces, maxSamples) 

        # create labels
        labels = [subdir for _ in range(len(faces))]
        # summarize progress
        print('>loaded %d examples for class: %s' % (len(faces), subdir))
        # store
        X.extend(faces)         # append 
        y.extend(labels)        #  ...
    return asarray(X), asarray(y)


def showFaces (faces):
    nFaces = len (faces)
    pyplot.figure()
    nRows = 2
    nCols = nFaces // nRows
    for (idx,face) in enumerate(faces):
        pyplot.subplot (2, nCols, idx+1)
        pyplot.axis('off')
        print ("idx = ", idx)
        pyplot.imshow(face)


def init ():
    pandarallel.initialize()
    tf.get_logger().setLevel(logging.ERROR)
    
        
def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("vipDir", help="The VIPs directory")
    parser.add_argument ("faceDataFile", help="The output face dataset pickle file")
    parser.add_argument ("maxClassExamples", nargs='?',
                         help="Optional max examples per class limit")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")

    # Specify Example:
    parser.epilog='''Example:
        %s vips_with_profile faces-dataset.pkl 
        %s vips_with_profile faces-dataset.pkl 10
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
    print ()
    print ("vipDir = ", args.vipDir)
    print ("faceDataFile = ", args.faceDataFile)
    print ("maxClassExamples = ", args.maxClassExamples)
    input ("press enter to continue")


    #---------------------------
    # run the program :
    #---------------------------
    # pandarallel.initialize()
    # tf.get_logger().setLevel(logging.ERROR)
    init ()
    if (args.maxClassExamples != None):
        maxExamples = int (args.maxClassExamples)
        print (f"max data limited to {maxExamples} per class")
        faceDataset = LoadFaceDataset (args.vipDir, maxExamples)
    else:
        print ("use all data")
        faceDataset = LoadFaceDataset (args.vipDir)
    pickle.dump (faceDataset, open (args.faceDataFile, 'wb'))
    
    #---------------------------
    # program termination:
    #---------------------------
    print ("wrote to ", args.faceDataFile)
    print ("Program Terminated Properly\n", flush=True)



