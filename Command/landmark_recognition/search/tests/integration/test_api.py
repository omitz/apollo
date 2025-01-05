import unittest
import pickle
import os
from apollo import PostgresDatabase
from apollo.models import Landmark
print('Importing create_app', flush=True)
from search.api import create_app
from search.api.api import FindResource

class TestSearch(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        print('\nRunning test_api.py', flush=True)
        os.environ['POSTGRES_USER'] = "postgres"
        os.environ['POSTGRES_HOST'] = "postgres"
        os.environ['POSTGRES_PASSWORD'] = "secretpassword"

        cls.database = PostgresDatabase('apollo', Landmark.__table__)
        # Drop any existing landmark table and create it again
        if cls.database.has_table(Landmark.__table__):
            cls.database.delete_all_from_table(Landmark)
        cls.database.create_table_if_not_exists(Landmark.__table__)

        # We'll use db.pkl to build up the database with real delf arrays without having to run that computation.
        # The images in db.pkl are worcester 000055, all_souls 0-10
        with open('tests/db.pkl', 'rb') as f:
            db = pickle.load(f)
        for landmark_dict in db:
            path = landmark_dict['path']
            print(f'Saving {path} to database', flush=True)
            cls.database.save_record_to_database(landmark_dict, Landmark)
        print('Creating flask app')
        cls.flask_app = create_app('search.config.Config')

        cls.msg_key = 'msg'
        cls.landmarks_key = 'landmarks'

    @classmethod
    def tearDownClass(cls):
        if cls.database.has_table(Landmark.__table__):
            cls.database.delete_all_from_table(Landmark)
        cls.database.close()

    @unittest.skipIf(os.getenv('JENKINS'), "Test does not work in jenkins because a file needs to be downloaded from S3.")
    def test_FindResource_get_no_res(self):
        '''
        Test case where there should not be any matches.
        '''
        print('Running test_FindResource_get_no_res')
        with self.flask_app.test_client() as client:
            response = client.get('/search/?name=s3://apollo-source-data/inputs/landmark/all_souls_000011.jpg')
        json_response = response.json
        self.assertEqual(json_response[self.msg_key], 'No matches.')

    @unittest.skipIf(os.getenv('JENKINS'), "Test does not work in jenkins because a file needs to be downloaded from S3.")
    def test_FindResource_get_res(self):
        '''
        Test case where there should be matches.
        '''
        print('Running test_FindResource_get_res')
        with self.flask_app.test_client() as client:
            response = client.get('/search/?name=s3://apollo-source-data/inputs/landmark/worcester_000194.jpg')
        json_response = response.json
        self.assertEqual(json_response[self.landmarks_key][0]['path'], 's3://apollo-source-data/inputs/landmark/worcester_000055.jpg')

    def test_FindResource_get_no_res_wo_download(self):
        '''
        Test case where there should not be any matches.
        '''
        with self.flask_app.app_context():
            find_resource = FindResource()
            response = find_resource.match('search/tests/all_souls_000011.jpg', 's3/all_souls_000011.jpg')
            json_response = response.json
        self.assertEqual(json_response[self.msg_key], 'No matches.')

    def test_FindResource_get_res_wo_download(self):
        '''
        Test case where there should be matches.
        '''
        with self.flask_app.app_context():
            find_resource = FindResource()
            response = find_resource.match('search/tests/worcester_000194.jpg', 's3/worcester_000194.jpg')
            json_response = response.json
            print(f'json response: {json_response}', flush=True)
        self.assertEqual(json_response[self.landmarks_key][0]['path'], 's3://apollo-source-data/inputs/landmark/worcester_000055.jpg')

    def test_FindResource_no_feats_in_query(self):
        '''
        Test case where the query image has no features (according to DELF).
        '''
        with self.flask_app.app_context():
            find_resource = FindResource()
            response = find_resource.match('search/tests/nothing.png', 's3/nothing.png')
            json_response = response.json
        self.assertIn('No features', json_response[self.msg_key])