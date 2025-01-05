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
import math
from PIL import Image
import numpy as np
from mtcnn.mtcnn import MTCNN
from libsvm.svmutil import svm_load_model, svm_predict
from scanf import scanf

from tensorflow.python.platform import gfile
import tensorflow.compat.v1 as tf
tf.disable_v2_behavior()
import logging

#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------

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
    results = detector.detect_faces (pixels)


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
    # image = image.resize (required_size, Image.NEAREST) # scale it
    image = image.resize (required_size, Image.BILINEAR) # match the phone quality
    # image = image.resize (required_size) # scale it
    face_array = np.asarray(image)
    # face_array = np.asarray(image, dtype=np.float32)
    return face_array


def prewhiten(x):
    """
    from facenet/src/facenet.py
    """
    mean = np.mean(x)
    print ("mean = ", mean)
    std = np.std(x)
    std_adj = np.maximum(std, 1.0/np.sqrt(x.size))
    y = np.multiply (np.subtract(x, mean), 1/std_adj)
    return y
    # return np.ones_like (y)    # TBF


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


def get_embeddings (faces, batch_size=90):
    """
    get the face embedding for one face

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
            load_model ("20180402-114759/20180402-114759.pb")
            
            # Get input and output tensors
            images_placeholder = tf.get_default_graph().get_tensor_by_name("input:0")
            embeddings = tf.get_default_graph().get_tensor_by_name("embeddings:0")
            phase_train_placeholder = tf.get_default_graph().get_tensor_by_name("phase_train:0")
            embedding_size = embeddings.get_shape()[1]
            print ("embedding_size is =", embeddings.get_shape) # shape=(?, 512) dtype=float32
            # Run forward pass to calculate embeddings
            print('Calculating features for images')
            emb_array = np.zeros ((nrof_images, embedding_size), dtype=np.float32)
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


def saveToPPM (filename, pixels, width, height):

    # First write the header values
    ppmfile = open (filename,'wb+') # note the binary flag
    ppmfile.write(b"P6\n") 
    ppmfile.write(b"%d %d\n" % (width, height)) 
    ppmfile.write(b"255\n")

    # Then loop through the screen and write the values
    pixels1D = pixels.ravel()
    for row in range(height):
        for col in range(width):
            red = pixels1D [(row * width + col) * 3]
            green = pixels1D [((row * width + col) * 3) + 1]
            blue = pixels1D [((row * width + col) * 3) + 2]
            ppmfile.write(b"%c%c%c" % (red,green,blue))
    ppmfile.close()


def loadFromPPM (filename):

    # First write the header values
    ppmfile = open (filename,'rb') # note the binary flag
    ppmType = ppmfile.readline().decode("utf-8").strip()
    dim_str = ppmfile.readline().decode("utf-8").strip()
    maxVal = int (ppmfile.readline().decode("utf-8").strip())

    (width, height) = scanf ("%d %d", dim_str)

    # allocate image storage
    pixels2D = np.zeros ((width,height,3), dtype=np.uint8)
    
    # read in the rest of byte-array
    pixels1D = ppmfile.read()
    ppmfile.close()

    # Then loop through the screen and read the values
    for row in range (height):
        for col in range (width):
            pixels2D [row,col,0] = pixels1D [(row * width + col) * 3]
            pixels2D [row,col,1] = pixels1D [((row * width + col) * 3) + 1]
            pixels2D [row,col,2] = pixels1D [((row * width + col) * 3) + 2]

    return pixels2D


def parseCommandLine ():
    description="""
    This progrma does whatever...
    """
    parser = argparse.ArgumentParser(
        description=textwrap.fill(description, 80),
        formatter_class = argparse.RawDescriptionHelpFormatter)

    # Specify Arguments:
    parser.add_argument ("faceImgFile", help="Image file containing a face")
    parser.add_argument ("libsvmFile", help="libsvm classifier")
    parser.add_argument ("faceLabelFile", help="face label mapping file")
    parser.add_argument ("-d", "--debugMode", help="debug mode value")

    # Specify Example:
    parser.epilog='''Example:
        %s jim_gaffigan.jpg model.desktop label
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
    # Create a parser:
    args = parseCommandLine ()

    # Access the options:
    print ()
    print ("faceImgFile = ", args.faceImgFile)
    print ("libsvmFile = ", args.libsvmFile)
    print ("faceLabelFile = ", args.faceLabelFile)
    input ("press enter to continue")

    #---------------------------
    # run the program :
    #---------------------------
    tf.get_logger().setLevel (logging.ERROR)
    
    ## get face_pixels
    face_pixels = extract_face (args.faceImgFile) # output should be 160x160x3
    print ("face_pixels = ", face_pixels[0,0,:], face_pixels[0,1,:])
    # (height, width) = face_pixels.shape[0:2]
    # saveToPPM ("crop_desktop.ppm", face_pixels, width, height)
    # saveToPPM ("crop_desktop_filter.ppm", face_pixels, width, height)
    
    # face_pixels = loadFromPPM ("crop.ppm")
    # print ("face_pixels = ", face_pixels[0,0,:], face_pixels[0,1,:])

    
    ## get embedding
    embedding = get_embeddings ([face_pixels], 1)[0]
    print ("embedding ", embedding[0], embedding[1], embedding[2],
           embedding[3], embedding[4], embedding[5])
    print ("embedding mean = ", embedding.mean())

    ## predict the face (use scikit->libsvm)
    m2 = svm_load_model (args.libsvmFile)
    face_labels = [line.strip() for line in open (args.faceLabelFile, "r").readlines()]

    # ## predict using the origiginal Edge model
    # m2 = svm_load_model ("scaffold/faceID.model")
    # face_labels = [line.strip() for line in open ("scaffold/faceID.label", "r").readlines()]
    
    ## predict the face (use direct libsmv)
    # m2 = svm_load_model ("libsvm_v2.model")
    # face_labels = [line.strip() for line in open ("libsvm_v2.label", "r").readlines()]


    ## Get the prediction
    p_label, p_acc, p_val = svm_predict ([], [embedding], m2, '-b 1') # probability
    maxPval_idx = np.argmax (p_val[0])
    # note, p_label is just a id, it has nothing to do with index into
    # p_val...  id = 4, but 4 was the first label
    print ("Face is ", face_labels[maxPval_idx])
    print ("Prob is ", p_val[0][maxPval_idx])
    
    #---------------------------
    # program termination:
    #---------------------------
    print ("Program Terminated Properly\n", flush=True)



