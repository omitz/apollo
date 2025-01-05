from unittest import TestCase
from unittest.mock import MagicMock
import docker, os
import sqlalchemy
import numpy as np
from obj_det.models.research.object_detection.inference import format_for_postgres, read_image
from apollo.models import DetectedObj
from apollo import PostgresDatabase


class TestInference(TestCase):
    @classmethod
    def setUpClass(cls):
        os.environ['RABBITMQ_HOST'] = "rabbitmq"
        os.environ['POSTGRES_HOST'] = "postgres"
        os.environ['POSTGRES_PASSWORD'] = "secretpassword"
        
    def setUp(cls):
        cls.db = PostgresDatabase(table=DetectedObj.__table__, echo=False)

    def tearDown(cls):
        cls.db.delete_all_from_table(DetectedObj)

    @classmethod
    def tearDownClass(cls):
        pass

    def test_format_for_postgres(self):
        '''
        Test that format_for_postgres creates the same keys as the columns in the postgres table.
        '''
        args = MagicMock()
        category_index = MagicMock()
        output_dict = MagicMock()
        reduced_dict = format_for_postgres(args, category_index, output_dict)
        # Assert all keys are columns in the postgres table
        postgres_cols = list(DetectedObj._sa_class_manager._all_key_set)
        for k, _ in reduced_dict.items():
            self.assertIn(k, postgres_cols)

    def test_read_image(self):
        img = read_image('obj_det/tests/test_files/puppies.png')
        self.assertEqual(np.ndarray, type(img))

    def test_read_image_empty(self):
        self.assertRaises(Exception, read_image, 'obj_det/tests/test_files/empty.png')

    def test_read_image_bad_path(self):
        self.assertRaises(FileNotFoundError, read_image, 'some_image_that_doesnt_exist.png')
