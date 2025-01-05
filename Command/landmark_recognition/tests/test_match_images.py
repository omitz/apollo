import unittest
from unittest.mock import MagicMock
import pickle
from scipy import spatial
import time
import numpy as np
import os
from apollo import models, PostgresDatabase
from match_images import calc_all_inliers_wo_dask, calc_all_inliers, select_putative_matches, get_sorted_matches
from extract_features import extract_feats


class TestMatchImages(unittest.TestCase):

    @unittest.skipIf(os.getenv('JENKINS'), "Test does not work in jenkins.\nThis test has to be run manually. It will fail when run with `docker-compose run landmark python -m unittest`.\nTo run this test, get a shell inside the docker container and run `python -m unittest`.")
    def test_dask_runtime(self):
        '''
        Test that Dask is configured correctly (ie faster than not using dask) in match_images.
        '''
        with open('tests/db.pkl', 'rb') as f:
            db_dicts = pickle.load(f)
        db = []
        for landmark_dict in db_dicts:
            instance = models.Landmark(**landmark_dict)
            db.append(instance)
        with open('tests/query.pkl', 'rb') as f:
            query = pickle.load(f)
            query = models.Landmark(**query)
        query_tree = spatial.cKDTree(query.delf_descriptors)

        start_without = time.time()
        all_num_inliers_wo_dask = calc_all_inliers_wo_dask(db, query, query_tree)
        end_without = time.time()
        wo_dask_runtime = end_without - start_without
        print(f'Without dask runtime: {wo_dask_runtime}')

        start_with = time.time()
        all_num_inliers_w_dask = calc_all_inliers(db, query, query_tree)
        end_with = time.time()
        w_dask_runtime = end_with - start_with
        print(f'With dask runtime: {w_dask_runtime}')

        assert w_dask_runtime < wo_dask_runtime

    def test_dask_order(self):
        '''
        Test that Dask appends elements to the list in the correct order.
        '''
        with open('tests/db.pkl', 'rb') as f:
            db_dicts = pickle.load(f)
        db = []
        for landmark_dict in db_dicts:
            instance = models.Landmark(**landmark_dict)
            db.append(instance)
        with open('tests/query.pkl', 'rb') as f:
            query = pickle.load(f)
            query = models.Landmark(**query)
        query_tree = spatial.cKDTree(query.delf_descriptors)
        all_num_inliers_wo_dask = calc_all_inliers_wo_dask(db, query, query_tree)
        all_num_inliers_w_dask = calc_all_inliers(db, query, query_tree)

        self.assertListEqual(all_num_inliers_wo_dask, all_num_inliers_w_dask)

    def test_select_putative_matches(self):
        '''
        Test the function that selects putative location (ie x, y) matches between the query image and an image (aka database element) in the database.
        Returns:

        '''
        query = MagicMock()
        db_el = MagicMock()
        # Make some fake DELF arrays for the query image
        query.delf_descriptors = [[1, 2, 3],
                       [4, 5, 6],
                       [7, 8, 9],
                       [10, 11, 12]]
        query_tree = spatial.cKDTree(query.delf_descriptors)
        # Make some fake DELF arrays for the image in the database where the first two arrays are the same as the query image
        db_el.delf_descriptors = [[1, 2, 3],
                             [4, 5, 6],
                             [0, 0, 0]]
        # Get the indices of the nearest neighbor matches between the two
        _, indices = query_tree.query(
            db_el.delf_descriptors, distance_upper_bound=.8)
        # Make some fake x, y locations in each image (one for each of the DELF arrays created above).
        query.delf_locations = [[1, 1],
                     [4, 4],
                     [7, 7],
                     [10, 10]]
        db_el.delf_locations = [[11, 11],
                           [44, 44],
                           [0, 0]]
        # The first two DELF arrays for each image are the same, so we expect select_putative_matches to return the first two location arrays for each image.
        query_locations_to_use, db_el_locations_to_use = select_putative_matches(indices, query, db_el, debug=True)
        query_correct = [[1, 1], [4, 4]]
        db_el_correct = [[11, 11], [44, 44]]
        assert np.array_equal(query_locations_to_use, np.array(query_correct))
        assert np.array_equal(db_el_locations_to_use, np.array(db_el_correct))

    def test_get_sorted_matches(self):
        '''
        Test that match images return the most similar image. worcester_000055 and worcester_000194 are pictures of the same building.
        '''
        # Add a few images to the database
        database = PostgresDatabase('apollo', models.Landmark.__table__)
        image_paths = ['tests/all_souls_000011.jpg', 'tests/worcester_000055.jpg', 'tests/worcester_000194.jpg']
        source_paths = ['s3/all_souls_000011.jpg', 's3/worcester_000055.jpg', 's3/worcester_000194.jpg']
        img_dicts, features_found = extract_feats(image_paths, source_paths)
        for d in img_dicts:
            database.save_record_to_database(d, models.Landmark)

        query = database.query(models.Landmark, models.Landmark.path == 's3/worcester_000055.jpg').first()
        high_to_low = get_sorted_matches(query)
        # Check that the first result is itself, then worcester_000194, then all_souls_000011.
        paths = [res[1].path for res in high_to_low]
        self.assertListEqual(['s3/worcester_000055.jpg', 's3/worcester_000194.jpg', 's3/all_souls_000011.jpg'], paths)

        # Cleanup
        database.delete_all_from_table(models.Landmark)
        database.close()