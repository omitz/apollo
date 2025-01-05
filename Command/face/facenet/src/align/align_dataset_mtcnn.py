"""Performs face alignment and stores face thumbnails in the output directory."""
# MIT License
# 
# Copyright (c) 2016 David Sandberg
# 
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
# 
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from scipy import misc
import sys
import os
import tensorflow as tf
import numpy as np
import face.facenet.src.facenet as facenet
from . import detect_face
import random


def detect(img):

    output_dir = 'src/mtcnn_result'
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    print('Creating networks and loading parameters')
    
    with tf.Graph().as_default():
        sess = tf.Session()
        with sess.as_default():
            pnet, rnet, onet = detect_face.create_mtcnn(sess, None)
    
    minsize = 20 # minimum size of face
    threshold = [ 0.6, 0.7, 0.7 ]  # three steps's threshold
    factor = 0.709 # scale factor

    bounding_boxes_filename = os.path.join(output_dir, 'bounding_boxes.txt')

    with open(bounding_boxes_filename, "w") as text_file:
        nrof_images_total = 0
        nrof_successfully_aligned = 0
        nrof_images_total += 1
        filename = os.path.splitext(os.path.split(img)[1])[0]
        output_filename = os.path.join(output_dir, filename+'.png')
        if not os.path.exists(output_filename):
            try:
                img = misc.imread(img)
            except (IOError, ValueError, IndexError) as e:
                errorMessage = '{}: {}'.format(img, e)
                print(errorMessage, flush=True)
            else:
                if img.ndim<2:
                    print('Unable to align "%s". ndim<2. : ' % img)
                    text_file.write('%s\n' % (output_filename))
                if img.ndim == 2:
                    img = facenet.to_rgb(img)
                img = img[:,:,0:3]

                bounding_boxes, _ = detect_face.detect_face(img, minsize, pnet, rnet, onet, threshold, factor)
                nrof_faces = bounding_boxes.shape[0]
                if nrof_faces>0:
                    det = bounding_boxes[:,0:4]
                    det_arr = []
                    img_size = np.asarray(img.shape)[0:2]
                    if nrof_faces>1:
                        for i in range(nrof_faces):
                            det_arr.append(np.squeeze(det[i]))
                        else:
                            bounding_box_size = (det[:,2]-det[:,0])*(det[:,3]-det[:,1])
                            img_center = img_size / 2
                            offsets = np.vstack([ (det[:,0]+det[:,2])/2-img_center[1], (det[:,1]+det[:,3])/2-img_center[0] ])
                            offset_dist_squared = np.sum(np.power(offsets,2.0),0)
                            index = np.argmax(bounding_box_size-offset_dist_squared*2.0) # some extra weight on the centering
                            det_arr.append(det[index,:])
                    else:
                        det_arr.append(np.squeeze(det))

                    for i, det in enumerate(det_arr):
                        det = np.squeeze(det)
                        bb = np.zeros(4, dtype=np.int32)
                        margin = 44
                        bb[0] = np.maximum(det[0]-margin/2, 0)
                        bb[1] = np.maximum(det[1]-margin/2, 0)
                        bb[2] = np.minimum(det[2]+margin/2, img_size[1])
                        bb[3] = np.minimum(det[3]+margin/2, img_size[0])
                        cropped = img[bb[1]:bb[3],bb[0]:bb[2],:]
                        image_size = 182
                        scaled = misc.imresize(cropped, (image_size, image_size), interp='bilinear')
                        nrof_successfully_aligned += 1
                        filename_base, file_extension = os.path.splitext(output_filename)
                        output_filename_n = "{}_{}{}".format(filename_base, i, file_extension)

                        # Make sure this name hasn't already been used
                        if os.path.isfile(output_filename_n):
                            random_int = random.randint(0, sys.maxsize)
                            output_filename_n = "{}_{}_{}{}".format(filename_base, i, random_int, file_extension)

                        misc.imsave(output_filename_n, scaled)
                        text_file.write('%s,%d,%d,%d,%d\n' % (output_filename_n, bb[0], bb[1], bb[2], bb[3]))
                else:
                    print('No faces detected in "%s"' % filename)
                            
    print('Total number of images: %d' % nrof_images_total)
    print('Number of successfully aligned images: %d' % nrof_successfully_aligned)
    return output_dir
