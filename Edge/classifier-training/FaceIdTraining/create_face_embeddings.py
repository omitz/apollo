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
import pickle
from joblib import Memory


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
mem = Memory(cachedir='/tmp/joblib', verbose=1)
g_model = None

#-------------------------
# Private Implementations 
#-------------------------

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
    face_pixels = face_pixels.astype('float32')
    # standardize pixel values across channels (global)
    # mean, std = face_pixels.mean(), face_pixels.std()
    # face_pixels = (face_pixels - mean) / std
    face_pixels_white = prewhiten (face_pixels) # copied from facenet.py

    # transform face into one sample (same as reshape(1,ori_shape))
    samples = expand_dims (face_pixels_white, axis=0)
    
    # make prediction to get embedding
    yhat = model.predict(samples)
    return yhat[0]              # return shape (128,) intead of (1,128)


def init ():
    g_model = facenet.load_model('20180402-114759/20180402-114759.pb')
    
    
def TransformTrainData (trainX):
    newTrainX = [get_embedding (g_model, face_pixels) for face_pixels in trainX]
    return newTrainX
    
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
    # model = load_model('facenet_keras.h5')
    # model = load_model('facenet_20180408-102900.h5')
    # model = facenet.load_model('20180402-114759/20180402-114759.pb')
    init ()
    
    # load face data
    trainX, trainy = pickle.load(open ("faces-dataset.pkl", 'rb'))

    # convert each face in the train set to an embedding
    # newTrainX = [get_embedding (g_model, face_pixels) for face_pixels in trainX]
    newTrainX = TransformTrainData (trainX)

    # save embeddings
    pickle.dump ([newTrainX, trainy], open ("faces-embeddings.pkl", 'wb'))

    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)



