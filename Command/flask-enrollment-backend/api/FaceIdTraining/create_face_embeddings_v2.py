#! /usr/bin/env python3
#
# This version uses tensorflow directly.
#
# ref: https://github.com/davidsandberg/facenet
#
# TC 2021-05-11 (Tue) 


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import argparse
import textwrap
import numpy as np
import math
from tensorflow.python.platform import gfile
import tensorflow.compat.v1 as tf
tf.disable_v2_behavior()
import pickle
import logging


#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------

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


def load_model (modelFile, input_map=None):
    # Check if the model is a model directory (containing a metagraph and a checkpoint file)
    #  or if it is a protobuf file with a frozen graph
    assert (os.path.isfile (modelFile))
    print ('Model filename: %s' % modelFile)

    with gfile.FastGFile (modelFile,'rb') as f:
        tensorflow_version_major = int(tf.__version__[0])
        if tensorflow_version_major == 2: # if using Tensorflow 2
            print ("using tensorflow 2")
            graph_def = tf.compat.v1.GraphDef()
            graph_def.ParseFromString(f.read())
            tf.compat.v1.import_graph_def(graph_def, input_map=input_map, name='')
        else:
            print ("using tensorflow 1")
            graph_def = tf.GraphDef()
            graph_def.ParseFromString(f.read())
            tf.import_graph_def(graph_def, input_map=input_map, name='')


def GetEmbeddings (faces, facenetModelFile="20180402-114759/20180402-114759.pb", batch_size=90):
    """
    get the face embedding for each face

    faces: each element is a (160, 160, 3) image

    return: a collection of feature vectors / embeddings
    """
    nrof_images = len (faces)
    nrof_batches_per_epoch = int (math.ceil (1.0 * nrof_images / batch_size))
    print ("nrof_batches_per_epoch = ", nrof_batches_per_epoch)
    
    with tf.Graph().as_default():
        with tf.Session() as sess:
            np.random.seed (seed=np.random.randint(1000))
            
            print ('Number of images: %d' % len(faces))

            # Load the model
            print('Loading feature extraction model')
            load_model (facenetModelFile)
            
            # Get input and output tensors
            images_placeholder = tf.get_default_graph().get_tensor_by_name("input:0")
            embeddings = tf.get_default_graph().get_tensor_by_name("embeddings:0")
            phase_train_placeholder = tf.get_default_graph().get_tensor_by_name("phase_train:0")
            embedding_size = embeddings.get_shape()[1]
            print ("embedding_size is =", embeddings.get_shape) # shape=(?, 512) dtype=float32
            
            # Run forward pass to calculate embeddings
            print('Calculating features for images')
            emb_array = np.zeros ((nrof_images, embedding_size))
            for idx in range (nrof_batches_per_epoch):
                start_index = idx * batch_size
                end_index = min ((idx+1) * batch_size, nrof_images)
                print ("start_index = ", start_index)
                print ("end_index = ", end_index)
                images = [prewhiten (face_pixels)
                          for face_pixels in faces[start_index:end_index]]

                # images[0] = np.zeros_like (images[0])
                # print ("image size", images[0].shape)
                
                feed_dict = {images_placeholder:images, phase_train_placeholder:False}
                emb = sess.run (embeddings, feed_dict=feed_dict)
                # print ("emb is ", emb[0][0:6])
                emb_array [start_index:end_index, :] = emb
    return emb_array


def init ():
    tf.get_logger().setLevel (logging.ERROR)    


def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("faceDataFile", help="The input face dataset pickle file")
    parser.add_argument ("faceEmbeddingFile", help="The output face embedding pickle file")
    parser.add_argument ("facenetModelFile", nargs='?', help="The facenet ternsorflow file",
                         default="20180402-114759/20180402-114759.pb")
    parser.add_argument ("-v", "--verbose", help="enable verbose mode", 
                         action="store_true")

    # Specify Example:
    parser.epilog='''Example:
        %s faces-dataset.pkl faces-embeddings.pkl
        %s faces-dataset.pkl faces-embeddings.pkl 20180402-114759/20180402-114759.pb
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
    print ("faceDataFile = ", args.faceDataFile)
    print ("faceEmbeddingFile = ", args.faceEmbeddingFile)
    print ("facenetModelFile = ", args.facenetModelFile)
    input ("press enter to continue")

    #---------------------------
    # run the program :
    #---------------------------
    # load face data
    # tf.get_logger().setLevel (logging.ERROR)
    init ()
    trainX, trainy = pickle.load (open (args.faceDataFile, 'rb'))

    # convert each face in the train set to an embedding
    newTrainX = GetEmbeddings (trainX, args.facenetModelFile)

    # save embeddings
    pickle.dump ([newTrainX, trainy], open (args.faceEmbeddingFile, 'wb'))

    #---------------------------
    # program termination:
    #---------------------------
    print ("wrote to ", args.faceEmbeddingFile)
    print ("Program Terminated Properly\n", flush=True)

