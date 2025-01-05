import unittest
import os

from passlib.hash import sha256_crypt
from apollo import PostgresDatabase, Neo4jGraphDatabase
from apollo.models import VideoDetections, User

from api import create_app


class TestSearchByTagVideo(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.flask_app = create_app()
        cls.search_prefix = '/search'
        cls.video_prefix = '/tag_video/?tag='
        os.environ['NEO4J_HOST'] = 'neo4j'
        os.environ['NEO4J_AUTH'] = 'neo4j/neo4j-password'
        cls.neo4j = Neo4jGraphDatabase()
        cls.neo4j.wait_until_available()

        os.environ['POSTGRES_HOST'] = "postgres"
        os.environ['POSTGRES_PASSWORD'] = "secretpassword"
        cls.database = PostgresDatabase(table=VideoDetections.__table__)
        cls.database.delete_all_from_table(VideoDetections)
        # Put some dummy data in the postgres db
        formatted_analytic_results = [{'path': 's3://apollo-source-data/inputs/obj_det_vid/holo clip.mp4', 'detection_class': 'book', 'detection_score': 60.21, 'seconds': [(1, 6)]},
                                      {'path': 's3://apollo-source-data/inputs/obj_det_vid/holo clip.mp4', 'detection_class': 'person', 'detection_score': 98.65, 'seconds': [(6, 12)]},
                                      {'path': 's3://apollo-source-data/inputs/obj_det_vid/holo clip.mp4', 'detection_class': 'teddy bear', 'detection_score': 58.81, 'seconds': [(12, 12)]}]
        for obj_class in formatted_analytic_results:
            cls.database.save_record_to_database(obj_class, VideoDetections)
        
        with cls.flask_app.app_context():
            from api import db
            if not db.session.query(User).filter_by(username="user").first():
                non_admin_user = User("user", sha256_crypt.hash("user"), ["user"])
                db.session.add(non_admin_user)
                db.session.commit()
            #login as user
            cls.authorization_token = cls.flask_app.test_client().post("/login/", data='{"username": "user", "password": "user"}', headers={"Content-Type" : "application/json"}).json['authorization_token']

    @classmethod
    def tearDownClass(cls):
        cls.database.delete_all_from_table(VideoDetections)
        cls.database.close()

    def test_get_no_res(self):
        '''
        Test that a query with an unknown search tag returns a message.
        '''
        query = f'{self.search_prefix}{self.video_prefix}personafdksa'
        with self.flask_app.test_client() as client:
            headers = {'Authorization': f"Bearer {self.authorization_token}"}
            response = client.get(query, headers=headers).json
            self.assertIsInstance(response, str)

    def test_get_res(self):
        '''
        Test that a query with an known search tag only returns the correct s3 path.
        '''
        # Put some additional dummy data in the table
        formatted_analytic_results = [
            {'path': 's3://somefile.mp4', 'detection_class': 'book',
             'detection_score': 60.21, 'seconds': [(1, 6)]},
            {'path': 's3://somefile.mp4', 'detection_class': 'teddy bear',
             'detection_score': 58.81, 'seconds': [(12, 12)]}]
        for obj_class in formatted_analytic_results:
            self.database.save_record_to_database(obj_class, VideoDetections)

        query = f'{self.search_prefix}{self.video_prefix}person'
        with self.flask_app.test_client() as client:
            headers = { 'Authorization' : f"Bearer {self.authorization_token}" }
            response = client.get(query, headers=headers).json
            expected_result_paths = ['s3://apollo-source-data/inputs/obj_det_vid/holo clip.mp4']
            result_paths = [l['path'] for l in response['objects']]
            self.assertListEqual(expected_result_paths, result_paths)

    def test_get_query_param(self):
        with self.flask_app.app_context():
            from api.search import SearchByTagVideo
            sbtv = SearchByTagVideo()
            self.assertEqual('tag', sbtv.get_query_param(), 'Update UI with new query parameter.')

    def test_object_detection_json_key(self):
        with self.flask_app.app_context():
            from api.search import SearchByTagVideo
            expected = 'objects'
            sbtv = SearchByTagVideo()
            actual = sbtv.get_json_key()
            self.assertEqual(expected, actual, 'Update UI with new key.')

