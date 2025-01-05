import unittest
import os

from passlib.hash import sha256_crypt
from apollo import PostgresDatabase
from apollo.models import DetectedObj, User

from api import create_app


class TestSearchByTag(unittest.TestCase):
    @classmethod
    def setUpClass(cls):

        cls.flask_app = create_app()

        cls.search_prefix = '/search'
        cls.tag_prefix = '/tag/?tag='

        os.environ['POSTGRES_HOST'] = 'postgres'
        os.environ['POSTGRES_PASSWORD'] = 'secretpassword'
        
        with cls.flask_app.app_context():
            from api import db
            
            if not db.session.query(User).filter_by(username="user").first():
                non_admin_user = User("user", sha256_crypt.hash("user"), ["user"])
                db.session.add(non_admin_user)
                db.session.commit()
            #login as user
            cls.authorization_token = cls.flask_app.test_client().post("/login/", data='{"username": "user", "password": "user"}', headers={"Content-Type" : "application/json"}).json['authorization_token']

        cls.database = PostgresDatabase(table=DetectedObj.__table__)
        cls.database.delete_all_from_table(DetectedObj)
        # Put some dummy data in the postgres db
        formatted_analytic_results = [{
              'bb_ymin_xmin_ymax_xmax': [
                [
                  0.6265605092048645,
                  0.3333702087402344,
                  0.8106345534324646,
                  0.577551007270813
                ]
              ],
              'detection_class': 'dog',
              'detection_scores': [
                0.525006115436554
              ],
              'id': 4,
              'path': 's3://apollo-source-data/inputs/face/lat_38.8614264_long_-77.2179631_2020-03-05T16-19-53-0500.png'
            },
          {
              'bb_ymin_xmin_ymax_xmax': [
                  [
                      0.27433347702026367,
                      0.6526347398757935,
                      0.6620563864707947,
                      0.8839750289916992
                  ],
                  [
                      0.2743721604347229,
                      0.3319339454174042,
                      0.7456231713294983,
                      0.5267831683158875
                  ],
                  [
                      0.29244494438171387,
                      0.08730359375476837,
                      0.7581092119216919,
                      0.3536471128463745
                  ],
                  [
                      0.24787871539592743,
                      0.4976655840873718,
                      0.7394101619720459,
                      0.6806451678276062
                  ]
              ],
              'detection_class': 'dog',
              'detection_scores': [
                  0.7863941192626953,
                  0.6947268843650818,
                  0.6946858763694763,
                  0.6163931488990784
              ],
              'id': 1,
              'path': 's3://apollo-source-data/inputs/obj_det/puppies.jpeg'
          }
          ]
        for result in formatted_analytic_results:
            cls.database.save_record_to_database(result, DetectedObj)

    @classmethod
    def tearDownClass(cls):
        cls.database.delete_all_from_table(DetectedObj)
        cls.database.close()

    def test_order(self):
        '''
        Test that the query results are returned sorted descending by the first element in the detection_scores array
        '''
        query = f'{self.search_prefix}{self.tag_prefix}dog'
        with self.flask_app.test_client() as client:
            headers = {'Authorization': f"Bearer {self.authorization_token}"}
            response = client.get(query, headers=headers).json
            expected_result_paths = ['s3://apollo-source-data/inputs/obj_det/puppies.jpeg', 's3://apollo-source-data/inputs/face/lat_38.8614264_long_-77.2179631_2020-03-05T16-19-53-0500.png']
            result_paths = [l['path'] for l in response['objects']]
            self.assertListEqual(expected_result_paths, result_paths)