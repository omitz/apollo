import unittest

from api import create_app
from apollo.models import VideoDetections
from apollo import PostgresDatabase

class TestRequestWithArgs(object):

    def __init__(self, args):
        self.args = args

class TestUpload(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.flask_app = create_app()
        with cls.flask_app.app_context():
            from api.upload import PaginatedUploadResource, file_upload_parser
            resource = PaginatedUploadResource()
            resource.put_parser = file_upload_parser()
            resource.upload_file_type = 'image'
            resource.description = 'face'
            resource.url = 'http://' + cls.flask_app.config['FACE_SEARCH_HOST'] + ':82/find'
            resource.results_key = 'results'
            cls.resource = resource
        

    def test_paginated_upload_get_items_per_pages(self):

        with self.flask_app.app_context():
            request = TestRequestWithArgs({'items_per_page': 20, 'page': 1})
            num_items = self.resource.get_items_per_page(request)
            self.assertEqual(num_items, 20)

            request = TestRequestWithArgs({})
            num_items = self.resource.get_items_per_page(request)
            self.assertEqual(num_items, 30)

    def test_paginated_upload_get_num_pages(self):

        with self.flask_app.app_context():
            request = TestRequestWithArgs({'items_per_page': 20, 'page': 1})
            results = { 'results': range(100) }
            num_pages = self.resource.get_num_pages(request, results, 'results')
            self.assertEqual(num_pages, 5)

            request = TestRequestWithArgs({})
            num_pages = self.resource.get_num_pages(request, results, 'results')
            self.assertEqual(num_pages, 4)

            #less than one full page
            results = { 'results': range(1) }
            request = TestRequestWithArgs({})
            num_pages = self.resource.get_num_pages(request, results, 'results')
            self.assertEqual(num_pages, 1)

            #empty results
            results = { 'results': [] }
            request = TestRequestWithArgs({})
            num_pages = self.resource.get_num_pages(request, results, 'results')
            self.assertEqual(num_pages, 0)

    def test_paginated_upload_paginate_results(self):

        with self.flask_app.app_context():
            
            #non-default items_per_page
            request = TestRequestWithArgs({'items_per_page': 20, 'page': 0})
            results = { 'results': range(100) }
            paginated_results = self.resource.paginate_results(request, results, 'results')
            self.assertCountEqual(range(20), paginated_results['results'])

            #default items_per_page
            request = TestRequestWithArgs({})
            paginated_results =  self.resource.paginate_results(request, results, 'results')
            #no page number arg should return all results
            self.assertCountEqual(range(100), paginated_results['results'])

            #less than one full page
            results = { 'results': range(1) }
            request = TestRequestWithArgs({'page': 0})
            paginated_results =  self.resource.paginate_results(request, results, 'results')
            self.assertCountEqual([0], paginated_results['results'])

            #empty results
            results = { 'results': [] }
            request = TestRequestWithArgs({'page': 0})
            paginated_results =  self.resource.paginate_results(request, results, 'results')
            self.assertCountEqual([], paginated_results['results'])

            #page number higher than num results, empty results
            results = { 'results': range(60) }
            request = TestRequestWithArgs({'page': 10})
            paginated_results =  self.resource.paginate_results(request, results, 'results')
            self.assertCountEqual([], paginated_results['results'])