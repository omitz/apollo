import unittest
import os

from passlib.hash import sha256_crypt
from apollo import PostgresDatabase
from apollo.models import ClassifyScene, User

from api import create_app


class TestSearchBySceneClassTag(unittest.TestCase):
    @classmethod
    def setUpClass(cls):

        cls.flask_app = create_app()

        cls.search_prefix = '/search'
        cls.tag_prefix = 'scene_class/?tag='

        os.environ['POSTGRES_HOST'] = 'postgres'
        os.environ['POSTGRES_PASSWORD'] = 'secretpassword'

        with cls.flask_app.app_context():
            from api import db

            if not db.session.query(User).filter_by(username="user").first():
                non_admin_user = User("user", sha256_crypt.hash("user"), ["user"])
                db.session.add(non_admin_user)
                db.session.commit()
            # login as user
            cls.authorization_token = \
            cls.flask_app.test_client().post("/login/", data='{"username": "user", "password": "user"}',
                                             headers={"Content-Type": "application/json"}).json['authorization_token']

        cls.database = PostgresDatabase(table=ClassifyScene.__table__)
        cls.database.delete_all_from_table(ClassifyScene)
        # Put some dummy data in the postgres db
        formatted_analytic_results = cls.get_formatted_analytic_results()
        for result in formatted_analytic_results:
            cls.database.save_record_to_database(result, ClassifyScene)

    @classmethod
    def tearDownClass(cls):
        cls.database.delete_all_from_table(ClassifyScene)
        cls.database.close()

    def test_scene_search_get(self):
        '''
        Test that the `get` function supports pagination arguments.
        '''
        args = 'runway&page=1&items_per_page=2'
        query = os.path.join(self.search_prefix, self.tag_prefix) + args
        with self.flask_app.test_client() as client:
            headers = {'Authorization': f"Bearer {self.authorization_token}"}
            response = client.get(query, headers=headers).json
            # We're trying to look at the second page (in a situation where only two items are displayed per page).
            # So we should expect to get back the result that is the 3rd most likely to be a runway.
            expected_result_paths = ['maybe_a_runway.jpg']
            result_paths = [l['original_source'] for l in response['scenes']]
            self.assertListEqual(expected_result_paths, result_paths)

    @classmethod
    def get_formatted_analytic_results(cls):
        runway_int = 293
        formatted_analytic_results = [
            {'path': 'probably_a_runway.png',
             'class_hierarchy': 'indoor',
             'top_five_classes': [185, 61, runway_int, 202, 240],
             'original_source': 'probably_a_runway.png'},
            {'path': 'maybe_a_runway.jpg',
             'class_hierarchy': 'outdoor',
             'top_five_classes': [178, 170, 257, 319, runway_int],
             'original_source': 'maybe_a_runway.jpg'},
            {'path': 'definitely_a_runway.jpg',
             'class_hierarchy': 'outdoor',
             'top_five_classes': [187, runway_int, 14, 309, 128],
             'original_source': 'definitely_a_runway.jpg'}
            ]
        return formatted_analytic_results