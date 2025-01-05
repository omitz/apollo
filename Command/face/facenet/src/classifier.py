"""An example of how to use your own dataset to train a classifier that recognizes people.
"""
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

import tensorflow as tf
import numpy as np
import argparse
import os
import sys
import math
import pickle
from sklearn.svm import SVC
from tqdm import tqdm
from time import time
import src.facenet as fn


def main(args):

    dataset, emb_array, label_mapping, labels, unknown_label = extract_embeddings_array(args)

    classifier_filename_exp = os.path.expanduser(args.classifier_filename)

    class_names, predictions = save_predictions(classifier_filename_exp, emb_array)

    # For each image, the index of the most-likely person
    best_class_indices = np.argmax(predictions, axis=1)
    # For each image, the probability of that most-likely person
    best_class_probabilities = predictions[
        np.arange(len(best_class_indices)),
        best_class_indices]

    # Threshold
    best_class_indices_thresholded = list()
    for i, prob in tqdm(enumerate(best_class_probabilities)):
        if prob > .4:
            best_class_indices_thresholded.append(best_class_indices[i])
        else:
            best_class_indices_thresholded.append(unknown_label)

    # For each image, the most-likely person and the probability of that person
    for i in tqdm(range(len(best_class_indices))):
        print('%4d  %s: %.3f' % (i, class_names[best_class_indices[i]], best_class_probabilities[i]))

    accuracy = np.mean(np.equal(best_class_indices, labels))
    acc = 'Accuracy: %.3f\n' % accuracy
    print(acc)

    thresholded_acc = np.mean(np.equal(best_class_indices_thresholded, labels))
    thresholded_acc = 'Thresholded accuracy: %.3f\n' % thresholded_acc
    print(thresholded_acc)

    with open('classifier_output.txt', 'a') as text_file:
        text_file.write(acc)


def save_predictions(classifier_filename_exp, emb_array):
    # Classify images
    print('Testing classifier')
    with open(classifier_filename_exp, 'rb') as infile:
        (model, class_names) = pickle.load(infile)
    print('Loaded classifier model from file "%s"' % classifier_filename_exp)
    start = time()
    predictions = model.predict_proba(emb_array)
    end = time()
    prediction_time_per_sample = (end - start) / len(emb_array)
    print(f'Prediction time per sample: {prediction_time_per_sample} second(s).')
    np.save('predictions.npy', predictions)
    return class_names, predictions


def extract_embeddings_array(args):
    with tf.Graph().as_default():

        with tf.Session() as sess:

            np.random.seed(seed=args.seed)

            if args.use_split_dataset:
                print('Splitting dataset...')
                dataset_tmp = fn.get_dataset(args.data_dir)
                train_set, test_set = split_dataset(dataset_tmp, args.min_nrof_images_per_class,
                                                    args.nrof_train_images_per_class)
                if (args.mode == 'TRAIN'):
                    dataset = train_set
                elif (args.mode == 'CLASSIFY'):
                    dataset = test_set
            else:
                dataset = fn.get_dataset(args.data_dir)

            # Check that there are at least one training image per class
            for cls in dataset:
                assert (len(cls.image_paths) > 0, 'There must be at least one image for each class in the dataset')

            # Get the label mapping from the TRAINING dataset
            paths, labels, label_mapping = fn.get_image_paths_and_labels(dataset)

            print('Number of classes: %d' % len(dataset))

            if args.mode == 'CLASSIFY' and args.test_data_dir:
                print('Prepping test data only...')
                dataset = fn.get_dataset(args.test_data_dir)
                paths = []
                labels = []
                unknown_label = len(label_mapping) + 1
                for i in range(len(dataset)): # For each person in the dataset
                    paths += dataset[i].image_paths
                    try:
                        person_int = label_mapping[dataset[i].name]
                    except KeyError:
                        person_int = unknown_label
                    # Append a <whatever the class integer label is> for every picture of that person
                    labels += [person_int] * len(dataset[i].image_paths)

            print('Number of images: %d' % len(paths))

            # Load the model
            print('Loading feature extraction model')
            fn.load_model(args.model)

            # Get input and output tensors
            images_placeholder = tf.get_default_graph().get_tensor_by_name("input:0")
            embeddings_tensor = tf.get_default_graph().get_tensor_by_name("embeddings:0")
            phase_train_placeholder = tf.get_default_graph().get_tensor_by_name("phase_train:0")
            embedding_size = embeddings_tensor.get_shape()[1]  # 512
            print(f'Embedding size: {embedding_size}')

            if args.mode == 'TRAIN':
                embedding_path = 'classifier_intermediate_output/emb_array.npy'
                if not os.path.exists(embedding_path):
                    emb_array = calc_embeddings(args, embedding_size, embeddings_tensor, images_placeholder, paths,
                                                phase_train_placeholder, sess)
                    # Guard against path issues when running from bash script
                    try:
                        np.save(embedding_path, emb_array)
                    except FileNotFoundError:
                        np.save('/tmp/emb_array.npy', emb_array)
                else:
                    print('Loading previously extracted embeddings.')
                    emb_array = np.load(embedding_path)
            else:
                emb_array = calc_embeddings(args, embedding_size, embeddings_tensor, images_placeholder, paths,
                                            phase_train_placeholder, sess)
    return dataset, emb_array, label_mapping, labels, unknown_label


def calc_embeddings(args, embedding_size, embeddings, images_placeholder, paths, phase_train_placeholder, sess):
    # Run forward pass to calculate embeddings
    print('Calculating features for images')
    nrof_images = len(paths)
    nrof_batches_per_epoch = int(math.ceil(1.0 * nrof_images / args.batch_size))
    emb_array = np.zeros((nrof_images, embedding_size))
    for i in tqdm(range(nrof_batches_per_epoch)):
        start_index = i * args.batch_size
        end_index = min((i + 1) * args.batch_size, nrof_images)
        paths_batch = paths[start_index:end_index]
        images = fn.load_data(paths_batch, False, False, args.image_size)
        feed_dict = {images_placeholder: images, phase_train_placeholder: False}
        emb_array[start_index:end_index, :] = sess.run(embeddings, feed_dict=feed_dict)
    return emb_array


def split_dataset(dataset, min_nrof_images_per_class, nrof_train_images_per_class):
    train_set = []
    test_set = []
    for cls in dataset:
        paths = cls.image_paths
        # Remove classes with less than min_nrof_images_per_class
        if len(paths)>=min_nrof_images_per_class:
            np.random.shuffle(paths)
            train_set.append(fn.ImageClass(cls.name, paths[:nrof_train_images_per_class]))
            test_set.append(fn.ImageClass(cls.name, paths[nrof_train_images_per_class:]))
    return train_set, test_set

            
def parse_arguments(argv):
    parser = argparse.ArgumentParser()
    
    parser.add_argument('mode', type=str, choices=['TRAIN', 'CLASSIFY'],
        help='Indicates if a new classifier should be trained or a classification ' + 
        'model should be used for classification', default='CLASSIFY')
    parser.add_argument('data_dir', type=str,
        help='Path to the data directory containing aligned LFW face patches.')
    parser.add_argument('model', type=str, 
        help='Could be either a directory containing the meta_file and ckpt_file or a model protobuf (.pb) file')
    parser.add_argument('classifier_filename', 
        help='Classifier model file name as a pickle (.pkl) file. ' + 
        'For training this is the output and for classification this is an input.')
    parser.add_argument('--use_split_dataset', 
        help='Indicates that the dataset specified by data_dir should be split into a training and test set. ' +  
        'Otherwise a separate test set can be specified using the test_data_dir option.', action='store_true')
    parser.add_argument('--test_data_dir', type=str,
        help='Path to the test data directory containing aligned images used for testing.')
    parser.add_argument('--batch_size', type=int,
        help='Number of images to process in a batch.', default=90)
    parser.add_argument('--image_size', type=int,
        help='Image size (height, width) in pixels.', default=160)
    parser.add_argument('--seed', type=int,
        help='Random seed.', default=666)
    parser.add_argument('--min_nrof_images_per_class', type=int,
        help='Only include classes with at least this number of images in the dataset', default=20)
    parser.add_argument('--nrof_train_images_per_class', type=int,
        help='Use this number of images from each class for training and the rest for testing. Only relevant with --use_split_dataset.', default=10)
    
    return parser.parse_args(argv)


if __name__ == '__main__':
    args_list = sys.argv[1:]
    main(parse_arguments(args_list))
