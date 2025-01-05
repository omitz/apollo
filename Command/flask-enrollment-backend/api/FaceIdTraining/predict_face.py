#! /usr/bin/env python3
#
#
# TC 2021-05-07 (Fri) 


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
from numpy import expand_dims
from keras.models import load_model
from PIL import Image
import numpy as np
from mtcnn.mtcnn import MTCNN
import pickle
from joblib import Memory

import tensorflow as tf
import logging
tf.get_logger().setLevel(logging.ERROR)

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
#mem = Memory(cachedir='/tmp/joblib', verbose=1)

#-------------------------
# Private Implementations 
#-------------------------

def extract_face (filename, required_size=(160, 160)):
    """Extract Face

    filename: the absolute path to image file.

    Extract a single face (highest confident) from a given photograph
    """
    # load image from file and detect faces in the image
    image = Image.open (filename)
    image = image.convert ('RGB')
    pixels = np.asarray(image)
    # pixels = skimage.io.imread (filename)
    
    detector = MTCNN()
    results = detector.detect_faces(pixels)


    ## Pick the max confident face:
    bestResult = max (results, key=lambda e: e['confidence'])
    x1, y1, width, height = bestResult['box']
    # bug fix
    # x1, y1 = abs(x1), abs(y1)
    assert ((x1 >= 0) and (y1 >= 0))
    x2, y2 = x1 + width, y1 + height

    # extract the face and resize to model size
    face = pixels[y1:y2, x1:x2]
    image = Image.fromarray(face)
    image = image.resize (required_size)
    face_array = np.asarray(image)
    return face_array



def prewhiten(x):
    """
    from facenet/src/facenet.py
    """
    mean = np.mean(x)
    std = np.std(x)
    std_adj = np.maximum(std, 1.0/np.sqrt(x.size))
    y = np.multiply (np.subtract(x, mean), 1/std_adj)
    return y


def get_embedding (model, face_pixels):
    """
    get the face embedding for one face

    model : The facenet model
    face_pixels: shape of (160, 160, 3)

    return: a feature vector (eg. 128-D vector)
    """
    # scale pixel values
    print ("pixel values = ", face_pixels[0,0,:], face_pixels[0,1,:], face_pixels[0,2,:])
    
    face_pixels = face_pixels.astype ('float32')
    # standardize pixel values across channels (global)
    # mean, std = face_pixels.mean(), face_pixels.std()
    # face_pixels = (face_pixels - mean) / std
    face_pixels_white = prewhiten (face_pixels) # copied from facenet.py
    face_pixels_white = np.ones_like (face_pixels_white) * 35 # TC 2021-05-11 (Tue) --
    print (" len (face_pixels_white)", face_pixels_white.size)
    # transform face into one sample (same as reshape(1,ori_shape))
    samples = expand_dims (face_pixels_white, axis=0)
    
    # make prediction to get embedding
    yhat = model.predict (samples)
    return yhat[0]              # return shape (128,) intead of (1,128)


def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("arg1", help="argument1 here")
    parser.add_argument ("arg2", nargs='?', help="optional argument2 here")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")
    parser.add_argument ("vars", nargs='+', help="arbitrary many varialbes")

    # Specify Example:
    parser.epilog='''Example:
        %s a b --verbose v1 v2 v3
        %s --debugMode 1 a b v1 v2 v3
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
    # args = parseCommandLine ()

    # Access the options:


    #---------------------------
    # run the program :
    #---------------------------
    # load the facenet model
    #facenet_model = load_model('facenet_keras.h5')
    #facenet_model = load_model('facenet_20180408-102900.h5')
    facenet_model = load_model('facenet_20180402-114759.h5')
    
    ## get face_pixels
    face_pixels = extract_face ("jim_gaffigan.jpg")
    face_pixels = np.zeros_like (face_pixels, face_pixels[0].dtype) # TBF

    ## get embedding
    embedding = get_embedding (facenet_model, face_pixels)
    print ("embedding ", embedding[0], embedding[1], embedding[2],
           embedding[3], embedding[4], embedding[5])

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)



