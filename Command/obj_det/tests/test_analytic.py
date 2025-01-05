from unittest import TestCase
import os

import numpy as np
from apollo.models import DetectedObj
from apollo import PostgresDatabase
from apollo import S3FileStore

from obj_det.object_detection_rabbit_consumer.objectdetectionrabbitconsumer import ObjectDetectionRabbitConsumer
from obj_det.object_detection_analytic.objectdetectionanalytic import ObjectDetectionAnalytic


class TestAnalytic(TestCase):

    @classmethod
    def setUpClass(cls):
        os.environ['RABBITMQ_HOST'] = "rabbitmq"
        os.environ['POSTGRES_HOST'] = "postgres"
        os.environ['POSTGRES_PASSWORD'] = "secretpassword"
        
    def setUp(cls):
        cls.db = PostgresDatabase(table=DetectedObj.__table__, echo=False)
        cls.filestore = S3FileStore(url='http://localstack:4572')
        cls.analytic = ObjectDetectionAnalytic('object_detection', cls.filestore)
        cls.filestore.wait_until_available()
        cls.filestore.upload_file("obj_det/tests/test_files/empty.png", "inputs/empty.png")
        cls.filestore.upload_file("obj_det/tests/test_files/nothing.png", "inputs/nothing.png")
        cls.filestore.upload_file("obj_det/tests/test_files/3cats.jpg", "inputs/3cats.jpg")
        cls.filestore.upload_file("obj_det/tests/test_files/dogs5.jpg", "inputs/dogs5.jpg")
        cls.filestore.upload_file("obj_det/tests/test_files/puppies.png", "inputs/puppies.png")
        cls.filestore.upload_file("obj_det/tests/test_files/pizza.jpeg", "inputs/pizza.jpeg")

    def tearDown(cls):
        cls.db.delete_all_from_table(DetectedObj)

    @classmethod
    def tearDownClass(cls):
        pass

    def testInit(self):
        self.assertIsNotNone(self.analytic.filemanager)
        self.assertIsNotNone(self.analytic.filestore)

    #the following tests mainly are to make the sure the analytic runs without errors and produces results
    def testRunNoResults(self):
        results = self.analytic.run('s3://apollo-source-data/inputs/nothing.png')
        self.assertIsNotNone(results)

    def testRunDogs5(self): 
        results = self.analytic.run('s3://apollo-source-data/inputs/dogs5.jpg')
        self.assertIsNotNone(results)

    def testRun3Cats(self):
        results = self.analytic.run('s3://apollo-source-data/inputs/3cats.jpg')
        self.assertIsNotNone(results)
    
    def testRunPuppies(self):
        results = self.analytic.run('s3://apollo-source-data/inputs/puppies.png')
        self.assertIsNotNone(results)
    
    def test_run_multiple_classes(self):
        results = self.analytic.run('s3://apollo-source-data/inputs/pizza.jpeg')
        self.assertIsNotNone(results)

    
    