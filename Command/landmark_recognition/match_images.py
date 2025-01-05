# https://github.com/tensorflow/models/blob/master/research/delf/delf/python/examples/match_images.py
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
"""Matches two images using their DELF features.

The matching is done using feature-based nearest-neighbor search, followed by
geometric verification using RANSAC.

The DELF features can be extracted using the extract_features.py script.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import argparse
import os
import time
import matplotlib.image as mpimg  # pylint: disable=g-import-not-at-top
import matplotlib.pyplot as plt
import numpy as np
from scipy import spatial
from skimage import feature
from skimage import measure
from skimage import transform

# import tf and silence warnings (both silencing methods are necessary)
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
import tensorflow as tf
tf.compat.v1.logging.set_verbosity(tf.compat.v1.logging.ERROR)

from tqdm import tqdm

from dask import delayed, compute, visualize
# Configure Dask to execute computations with a local multiprocessing.Pool. Otherwise using Dask delayed will be slower than not using Dask.
import dask.multiprocessing
print(f'Configuring dask...')
dask.config.set(scheduler='processes')

from apollo import PostgresDatabase
from apollo.models import Landmark


def get_sorted_matches(query, debug=False):
    pg_db = PostgresDatabase('apollo', Landmark.__table__)
    query_tree = spatial.cKDTree(query.delf_descriptors)
    # # prev
    # db = session.query(models.Landmark).all()
    all_landmarks = pg_db.query(Landmark).all()
    if debug:
        all_num_inliers = calc_all_inliers_wo_dask(all_landmarks, query, query_tree)
    else:
        all_num_inliers = calc_all_inliers(all_landmarks, query, query_tree)
    zipped = zip(all_num_inliers, all_landmarks)
    high_to_low = sorted(zipped, key=lambda pair: -pair[0])
    pg_db.close()
    return high_to_low


def calc_all_inliers_wo_dask(db, query, query_tree):
    '''
    For each element in the database, calculate the number of inliers between the query image and the database image.
    Normally we do this using dask via calc_all_inliers, but if you need to step through this script with a debugger, it's better to use this function.
    '''
    all_num_inliers = []
    for db_el in tqdm(db):
        inliers, _, _ = get_neighbors_and_inliers(db_el, query, query_tree)
        all_num_inliers.append(sum(inliers))
    return all_num_inliers


def calc_all_inliers(db, query, query_tree):
    '''
    Use Dask to, for each element in the database, calculate the number of inliers between the query image and the database image.
    '''
    start_with = time.time()
    all_num_inliers_dask = []
    for db_el in db:
        lazy_inliers_and_locs_to_use = delayed(get_neighbors_and_inliers)(db_el, query, query_tree)
        lazy_num_inliers = delayed(sum)(lazy_inliers_and_locs_to_use[0])
        all_num_inliers_dask.append(lazy_num_inliers)
    all_num_inliers_dask = compute(*all_num_inliers_dask)
    all_num_inliers_dask = list(all_num_inliers_dask)  # Cast the result to a list bc Dask returns all_num_inliers as a (very long) tuple
    end_with = time.time()
    w_dask_runtime = end_with - start_with
    if len(db) > 0:
        print(f'runtime: {w_dask_runtime / len(db)} seconds per element in db')
    return all_num_inliers_dask


def get_neighbors_and_inliers(db_el, query, query_tree):
    indices = find_neighbors(query_tree, db_el)
    inliers, query_locations_to_use, db_el_locations_to_use = get_inliers(indices, query, db_el)
    return inliers, query_locations_to_use, db_el_locations_to_use


def find_neighbors(query_tree, db_el):
    '''
    Find nearest-neighbor matches using a KD tree. A K-D Tree (also called as K-Dimensional Tree) is a binary search tree where data in each node is a K-Dimensional point in space. In short, it is a space partitioning data structure for organizing points in a K-Dimensional space. In our case, K is 40.
    I.e. 'For each of the 1000 vectors in db_el_descriptors, find the closest match in the query_descriptors, and return the index of that match in the query_descriptors. If the vector has no matches, return n, where n is the length of query.descriptors.'
    The number of non-1000 indices in this list serves as a preliminary image-to-image similarity indicator.)
    '''
    _, indices = query_tree.query(
        db_el.delf_descriptors, distance_upper_bound=0.8)
    return indices


def get_inliers(indices, query, db_el):
    query_locations_to_use, db_el_locations_to_use = select_putative_matches(indices, query, db_el)
    query_num_features = len(query.delf_locations)
    min_samples = int(np.sqrt(query_num_features)) # Not sure what the best logic is to set this variable. The paper says they use the number of inliers to determine the top n matches, but RANSAC is expensive and the number of putative matches already gives us a reasonably good idea of whether or not a database image is a good match.
    if len(query_locations_to_use) <= min_samples:
        # Don't even bother running RANSAC
        inliers = [False]
    else:
        inliers = geometric_verification(min_samples, query_locations_to_use, db_el_locations_to_use)
        if inliers is None:
            inliers = [False]
    return inliers, query_locations_to_use, db_el_locations_to_use


def geometric_verification(min_samples, query_locations_to_use, db_el_locations_to_use):
    '''
    Perform geometric verification using RANSAC.
    Args:
        min_samples: The minimum number of data points to fit a model to; Usually 31
    Returns:
        inliers: A list of booleans
    '''
    max_trials = 500 # The original repo has this set at 1000. We've lowered it to make landmark/landmark-search faster. If we want to improve accuracy later, we should increase this back to 1000.
    _, inliers = measure.ransac((query_locations_to_use, db_el_locations_to_use),
                                transform.AffineTransform,
                                min_samples=min_samples,
                                residual_threshold=20, # Maximum distance for a data point to be classified as an inlier.
                                max_trials=max_trials, # Maximum number of iterations for random sample selection.
                                random_state=310) # We provide a random seed so that our results are consistent. (RANSAC is Random Sample Consensus, so otherwise it would give different answers)
    return inliers


def select_putative_matches(indices, query, db_el, debug=False):
    '''
    (For both images) Select feature locations for putative matches.
    Args:
        indices: Output of KD Tree query; See find_neighbors.
        query: The Landmark instance for the query image
        db_el: The Landmark instance for the image in the database we're comparing with
        debug: If True, use the traditional for-loop (instead of list comprehension). Helpful for understanding the indexing used in this function.
    Returns: np arrays: For each Landmark, the feature locations for which there is a match in the other Landmark
    '''
    num_query_features = len(query.delf_locations)
    num_db_el_features = len(db_el.delf_locations)
    if not debug:
        query_locations_to_use = np.array([np.array(query.delf_locations)[indices[i]] # original repo had [indices[i],]
            for i in range(num_db_el_features)
            if indices[i] != num_query_features
        ])
        db_el_locations_to_use = np.array([np.array(db_el.delf_locations)[i] # original repo had [i,]
            for i in range(num_db_el_features)
            if indices[i] != num_query_features
        ])
    else:
        query_locations_to_use = []
        for i in range(num_db_el_features):
            if indices[i] != num_query_features:
                loc = np.array(query.delf_locations)[indices[i]]
                query_locations_to_use.append(loc)
        query_locations_to_use = np.array(query_locations_to_use)
        db_el_locations_to_use = []
        for i in range(num_db_el_features):
            if indices[i] != num_query_features:
                loc = np.array(db_el.delf_locations)[i]
                db_el_locations_to_use.append(loc)
        db_el_locations_to_use = np.array(db_el_locations_to_use)
    return query_locations_to_use, db_el_locations_to_use


def check_channels(img):
    if img.shape[-1] == 4:
        img = img[:, :, :3]
    return img


def visualize_inliers(inliers, query, db_el, query_locations_to_use, db_el_locations_to_use):
    tf.compat.v1.logging.info('Found %d inliers' % sum(inliers))
    img_1 = mpimg.imread(query.path)
    print(f'database image path: {db_el.path}')
    img_2 = mpimg.imread(db_el.path)
    # Visualize plain images
    f, axarr = plt.subplots(1, 2)
    axarr[0].imshow(img_1)
    axarr[1].imshow(img_2)
    plt.show()
    # Visualize correspondences, and save to file.
    _, ax = plt.subplots()
    inlier_idxs = np.nonzero(inliers)[0]
    img_1 = check_channels(img_1)
    img_2 = check_channels(img_2)

    feature.plot_matches(
        ax,
        img_1,
        img_2,
        query_locations_to_use,
        db_el_locations_to_use,
        np.column_stack((inlier_idxs, inlier_idxs)),
        matches_color='b')
    ax.axis('off')
    ax.set_title('DELF correspondences')
    # plt.savefig(cmd_args.output_image) # TODO (low priority) Save out top results
    # plt.show()