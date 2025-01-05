from unittest import TestCase, skipIf
import pandas as pd
import os

from apollo import S3FileStore
from face.facenet_analytic.facenetanalytic import FacenetAnalytic
from face.facenet_analytic.facenetanalytic import S3_OUTPUT_DIR


@skipIf(os.getenv('JENKINS'), "these tests test the accuracy of the facenet module")
class TestFacenetAnalytic(TestCase):

    @classmethod
    def setUpClass(self):
        self.s3_filestore = S3FileStore(bucket_name="apollo-source-data")
        self.s3_filestore.wait_until_available()

        self.s3_filestore.upload_file("face/facenet_analytic/tests/test_files/kanye.png", "inputs/kanye.png")
        self.s3_filestore.upload_file("face/facenet_analytic/tests/test_files/ewan.jpg", "inputs/ewan.jpg")
        self.s3_filestore.upload_file("face/facenet_analytic/tests/test_files/ewan_and_daughter.jpeg", "inputs/ewan_and_daughter.jpeg")
        self.s3_filestore.upload_file("face/facenet_analytic/tests/test_files/ewan_and_others.jpg", "inputs/ewan_and_others.jpg")
        self.s3_filestore.upload_file("face/facenet_analytic/tests/test_files/multiple_faces.jpg", "inputs/multiple_faces.jpg")
        self.s3_filestore.upload_file("face/facenet_analytic/tests/test_files/not_mvp.jpg", "inputs/not_mvp.jpg")
        self.s3_filestore.upload_file("face/facenet_analytic/tests/test_files/chicken.jpg", "inputs/chicken.jpg")


    def setUp(self):
        self.facenet_analytic = FacenetAnalytic("facenet", self.s3_filestore)

    def test_init(self):
        self.assertEqual("facenet", self.facenet_analytic.name)
        self.assertIsNotNone(self.facenet_analytic.filestore)
        #test download model
        self.assertEqual("/dev/shm/clf.pkl", self.facenet_analytic.recog_model)

    def test_run_png(self):
        results = self.facenet_analytic.run("s3://apollo-source-data/inputs/kanye.png")

        self.assertEqual(results['dataframe'][0]['prediction'], 'Unknown')
        self.assertGreaterEqual(len(results['embedding_array']), 1)
        s3_key = os.path.join(S3_OUTPUT_DIR, 'kanye_command_result.png')
        self.assertTrue(self.s3_filestore.key_exists(s3_key))

        self.s3_filestore.delete_file(s3_key)

    def test_run_not_mvp(self):
        results = self.facenet_analytic.run("s3://apollo-source-data/inputs/not_mvp.jpg")
        self.assertEqual(results['dataframe'][0]['prediction'], 'Unknown')
        self.assertGreaterEqual(len(results['embedding_array']), 1)
        s3_key = os.path.join(S3_OUTPUT_DIR, 'not_mvp_command_result.png')
        self.assertTrue(self.s3_filestore.key_exists(s3_key))

        self.s3_filestore.delete_file(s3_key)

    def test_run_no_face(self):
        results = self.facenet_analytic.run("s3://apollo-source-data/inputs/chicken.jpg")
        self.assertEqual(results['dataframe'], {})
        self.assertEqual(len(results['embedding_array']), 0)

    def test_run_mvp(self):
        results = self.facenet_analytic.run("s3://apollo-source-data/inputs/ewan.jpg")
        self.assertEqual(results['dataframe'][0]['prediction'], 'Ewan_McGregor')
        self.assertGreaterEqual(len(results['embedding_array']), 1)
        s3_key = os.path.join(S3_OUTPUT_DIR, 'ewan_command_result.png')
        self.assertTrue(self.s3_filestore.key_exists(s3_key))

        self.s3_filestore.delete_file(s3_key)

    def test_run_mvp_and_other(self):
        results = self.facenet_analytic.run("s3://apollo-source-data/inputs/ewan_and_daughter.jpeg")
        predictions = [row['prediction'] for i, row in pd.DataFrame.from_dict(results['dataframe'], orient='index').iterrows()]
        self.assertEqual(len(predictions), 2)
        self.assertIn('Ewan_McGregor', predictions)
        self.assertIn('Unknown', predictions)
        self.assertGreaterEqual(len(results['embedding_array']), 1)

        s3_key = os.path.join(S3_OUTPUT_DIR, 'ewan_and_daughter_command_result.png')
        self.assertTrue(self.s3_filestore.key_exists(s3_key))

        self.s3_filestore.delete_file(s3_key)

    def test_run_mvp_and_others(self):
        results = self.facenet_analytic.run("s3://apollo-source-data/inputs/ewan_and_others.jpg")
        predictions = [row['prediction'] for i, row in pd.DataFrame.from_dict(results['dataframe'], orient='index').iterrows()]
        self.assertEqual(len(predictions), 6)
        self.assertIn('Ewan_McGregor', predictions)
        self.assertIn('Unknown', predictions)
        self.assertGreaterEqual(len(results['embedding_array']), 1)
        s3_key = os.path.join(S3_OUTPUT_DIR, 'ewan_and_others_command_result.png')

        self.s3_filestore.delete_file(s3_key)

    def test_run_multiple_unknown(self):
        results = self.facenet_analytic.run("s3://apollo-source-data/inputs/multiple_faces.jpg")
        self.assertGreaterEqual(len(results['dataframe']), 10)
        self.assertGreaterEqual(len(results['embedding_array']), 1)
        s3_key = os.path.join(S3_OUTPUT_DIR, 'multiple_faces_command_result.png')
        self.assertTrue(self.s3_filestore.key_exists(s3_key))

        self.s3_filestore.delete_file(s3_key)