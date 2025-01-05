# Copyright 2017 The TensorFlow Authors All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================
"""Extracts DELF features from a list of images, saving them to file.

The images must be in JPG format. The program checks if descriptors already
exist, and skips computation for those.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import argparse
import os
import sys
import time
from tqdm import tqdm

from six.moves import range

# import tf and silence warnings (both silencing methods are necessary)
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
import tensorflow as tf
tf.compat.v1.logging.set_verbosity(tf.compat.v1.logging.ERROR)

from google.protobuf import text_format
from protos import delf_config_pb2
import extractor


# Extension of feature files.
_DELF_EXT = '.delf'

# Pace to report extraction log.
_STATUS_CHECK_ITERATIONS = 100


def extract_feats(image_paths, source_paths=None):
    num_images = len(image_paths)
    parse_start = time.time()
    # Parse DelfConfig proto.
    config = delf_config_pb2.DelfConfig()
    with tf.io.gfile.GFile('delf_config_example.pbtxt', 'r') as f:
        text_format.Merge(f.read(), config)
    parse_finish = time.time()
    print(f'\nParse config time: {parse_finish - parse_start}')
    # Tell TensorFlow that the model will be built into the default Graph.
    with tf.Graph().as_default():
        # Reading list of images.
        filename_queue = tf.compat.v1.train.string_input_producer(
            image_paths, shuffle=False)
        reader = tf.compat.v1.WholeFileReader()
        _, value = reader.read(filename_queue)
        image_tf = tf.io.decode_jpeg(value, channels=3)

        with tf.compat.v1.Session() as sess:

            start = time.time()
            init_op = tf.compat.v1.global_variables_initializer()
            sess.run(init_op)
            extractor_fn = extractor.MakeExtractor(sess, config)
            # Start input enqueue threads.
            coord = tf.train.Coordinator()
            threads = tf.compat.v1.train.start_queue_runners(sess=sess, coord=coord)
            end = time.time()
            print(f'\nSteps in extract_features as sess: {end - start}')

            img_dicts = []
            for i in tqdm(range(num_images)):
                # Write to log-info once in a while.
                if i == 0:
                    tf.compat.v1.logging.info(
                        'Starting to extract DELF features from images...')

                # # Get next image.
                im = sess.run(image_tf)

                extractor_start = time.time()
                # Extract and save features.
                (locations_out, descriptors_out, feature_scales_out,
                 attention_out) = extractor_fn(im)
                extractor_end = time.time()
                print(f'\nExtractor_fn time: {extractor_end - extractor_start}', flush=True)

                if len(feature_scales_out) > 0:  # If any features were detected
                    img_dict = dict()
                    img_dict['path'] = source_paths[i]
                    img_dict['delf_locations'] = locations_out.tolist()
                    img_dict['delf_descriptors'] = descriptors_out.tolist()
                    img_dicts.append(img_dict)

            # Finalize enqueue threads.
            coord.request_stop()
            coord.join(threads)
    # img_dicts will be 0 if no features were extracted
    if len(img_dicts) > 0:
        features_found = True
    else:
        features_found = False

    # Generate pkl files for testing
    # for landmark_dict in img_dicts:
    #     path = landmark_dict['path']
    #     base = os.path.basename(path)
    #     src_path = os.path.join('s3://apollo-source-data/inputs/landmark/', base)
    #     landmark_dict['path'] = src_path
    # import pickle
    # with open('tests/db.pkl', 'wb') as f:
    #     pickle.dump(img_dicts, f, protocol=pickle.HIGHEST_PROTOCOL)
    # with open('tests/query.pkl', 'wb') as f:
    #     pickle.dump(img_dicts[0], f, protocol=pickle.HIGHEST_PROTOCOL)

    return img_dicts, features_found