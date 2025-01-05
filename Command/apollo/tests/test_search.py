from unittest import TestCase
import os
import sys

import boto3
import botocore
from flask import jsonify

from apollo import create_app
from apollo import Analytic
from apollo.models import DetectedObj

class TestSearch(TestCase):

    @classmethod
    def setUpClass(self):
        pass

    def setUp(self):
        analytic = FakeAnalytic("fake")
        self.app = create_app(analytic)
        self.client = self.app.test_client()

    def tearDown(self):
        pass

    @classmethod
    def tearDownClass(self):
        pass

    def test_create_app(self):
        self.assertEqual('fake', self.app.analytic.name)
        
        response = self.client.get("/health/")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.get_json(), {'hello': 'world'})

    def test_get(self):
        response = self.client.get('/find?name="s3://apollo-source-data/inputs/face/dalai.png"')
        self.assertEqual(response.status_code, 200)

        expected_result = [
            {
                'bb_ymin_xmin_ymax_xmax': None,
                'detection_class': None, 
                'detection_scores': None,
                'id': None, 
                'path': None,
                'original_source': None,
                'timestamp': None,
            },
            {
                'bb_ymin_xmin_ymax_xmax': None,
                'detection_class': None, 
                'detection_scores': None,
                'id': None, 
                'path': None,
                'original_source': None,
                'timestamp': None,
            }
        ]

        self.assertEqual(response.get_json()['results'], expected_result)

class FakeAnalytic(Analytic):

    def run():
        return {"analytic": "response"}
    
    def get_closest_results(self, file_name, num_results=10):
        obj1 = DetectedObj()
        obj2 = DetectedObj()
        return [obj1, obj2]

    def cleanup(self):
        pass
    