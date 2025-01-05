from unittest import TestCase
import os
import sys

import boto3
import botocore

from apollo import S3FileStore

class TestS3Filestore(TestCase):

    @classmethod
    def setUpClass(self):
        self.client = boto3.client('s3', region_name='us-east-1', endpoint_url='http://localstack:4572')
        self.s3_filestore = S3FileStore(url='http://localstack:4572')
        self.s3_filestore.wait_until_available()
        self.client.create_bucket(Bucket=self.s3_filestore.s3_bucket)

    def setUp(self):
        pass

    def tearDown(self):
        pass

    @classmethod
    def tearDownClass(self):
        pass

    def test_init(self):
        #check if bucket exists
        result = self.client.head_bucket(Bucket=self.s3_filestore.s3_bucket)
        self.assertIsNotNone(result)

    def test_upload_file(self):
        self.s3_filestore.upload_file('./tests/test_files/cats.jpg', '/cats.jpg')
    
        file_exists = self.s3_filestore.key_exists('/cats.jpg')
        self.assertTrue(file_exists)

    def test_file_exists(self):
  
        file_exists = self.s3_filestore.key_exists('/dogs.jpg')
        self.assertFalse(file_exists)

    def test_download_file(self):
        if os.path.exists('./cats.jpg'):
            os.remove('./cats.jpg')
        self.s3_filestore.upload_file('./tests/test_files/cats.jpg', '/cats.jpg')
        
        self.s3_filestore.download_file('/cats.jpg', './')

        self.assertTrue(os.path.exists('./cats.jpg'))
        os.remove('./cats.jpg')

