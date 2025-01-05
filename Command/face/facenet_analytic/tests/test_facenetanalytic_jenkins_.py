from unittest import TestCase
from unittest.mock import MagicMock
from apollo import PostgresDatabase
from apollo.models import DetectedFace
from face.facenet_analytic.facenetanalytic import FacenetAnalytic

class TestFacenetAnalyticJenkins(TestCase):
    '''
    Jenkins-friendly facenet analytic tests
    '''

    @classmethod
    def setUpClass(cls):
        mock_filestore = MagicMock()
        cls.analytic = FacenetAnalytic('facenet', mock_filestore)
        cls.analytic.database = PostgresDatabase('apollo')
        cls.analytic.database.create_table_if_not_exists(DetectedFace.__table__)
        cls.analytic.database.delete_all_from_table(DetectedFace)

    @classmethod
    def tearDownClass(cls):
        cls.analytic.database.delete_all_from_table(DetectedFace)

    def test_get_postgres_records(self):
        '''
        Test that when we query postgres with Milvus ids, we get back the correct image paths from postgres
        '''
        # Add some data to the face table
        rec1 = {'path': 's3://apollo-source-data/inputs/face/someone.png', 'ulx': 0.29873417721518986, 'uly': 0.0,
              'lrx': 0.7873417721518987, 'lry': 0.7932203389830509, 'probability': 0.9438227584875651,
              'prediction': 'Unknown', 'vector_id': 2}
        rec2 = {'path': 's3://apollo-source-data/inputs/face/dalai.png', 'ulx': 0.29873417721518986, 'uly': 0.0,
              'lrx': 0.7873417721518987, 'lry': 0.7932203389830509, 'probability': 0.9438227584875651,
              'prediction': '14th_Dalai_Lama', 'vector_id': 3}
        rec3 = {'path': 's3://apollo-source-data/inputs/face/someone.png', 'ulx': 0.49873417721518986, 'uly': 0.1,
              'lrx': 0.8873417721518987, 'lry': 0.8932203389830509, 'probability': 0.9438227584875651,
              'prediction': 'Unknown', 'vector_id': 4}
        for rec in [rec1, rec2, rec3]:
            self.analytic.database.save_record_to_database(rec, DetectedFace)

        # Pretend we ran a search that yielded the following milvus ids and distances
        milvus_results = [[ MilvusResult(3, 0.67),
                            MilvusResult(2, 1.56),
                            MilvusResult(4, 1.74)]]
        # Search postgres for those vector ids
        results = self.analytic.get_postgres_records(milvus_results[0])
        paths = [result.path for result in results]
        expected_paths = ['s3://apollo-source-data/inputs/face/dalai.png', 's3://apollo-source-data/inputs/face/someone.png', 's3://apollo-source-data/inputs/face/someone.png']
        self.assertCountEqual(paths, expected_paths) ##asserts lists are equal without regard to order

class MilvusResult():
    def __init__(self, id, distance):
        self.id = id
        self.distance = distance