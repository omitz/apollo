import unittest

from api import create_app
from apollo.models import VideoDetections
from apollo import PostgresDatabase


class TestSearch(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        cls.flask_app = create_app()
        cls.database = PostgresDatabase(table=VideoDetections.__table__)
        cls.database.delete_all_from_table(VideoDetections)

    def test_url_same(self):
        with self.flask_app.app_context():
            from api.search import search_blueprint
            expected = 'search'
            actual = search_blueprint.name
            self.assertEqual(expected, actual, 'Update UI with new endpoint.')

    def test_urls(self):
        with self.flask_app.app_context():
            available_urls = [str(p) for p in self.flask_app.url_map.iter_rules()]
            used_by_ui = ['/search/tag/', '/search/tag_video/', '/search/ner/', '/search/full_text/', '/search/scene_hierarchy/', '/search/scene_class/']
            for url in used_by_ui:
                self.assertIn(url, available_urls, 'Update UI with new endpoint.')

    def test_query_tag(self):
        with self.flask_app.app_context():
            from api.search import SearchByTag, SearchBySceneHierarchy, SearchBySceneClassTag
            expected = 'tag'
            for resource in [SearchByTag, SearchBySceneHierarchy, SearchBySceneClassTag]:
                resource_instance = resource()
                actual = resource_instance.get_query_param()
                msg = f'Update UI with new query parameter for {resource}.'
                self.assertEqual(expected, actual, msg)

    def test_query_ner(self):
        with self.flask_app.app_context():
            from api.search import SearchByEntity
            expected = 'entity'
            sbe = SearchByEntity()
            actual = sbe.get_query_param()
            self.assertEqual(expected, actual, 'Update UI with new query parameter for NER.')

    def test_query_fulltext(self):
        with self.flask_app.app_context():
            from api.search import SearchInFullText
            expected = 'query'
            sft = SearchInFullText()
            actual = sft.get_query_param()
            self.assertEqual(expected, actual, 'Update UI with new query parameter for full text search.')

    def test_object_detection_json_key(self):
        with self.flask_app.app_context():
            from api.search import SearchByTag
            expected = 'objects'
            sbt = SearchByTag()
            actual = sbt.get_json_key()
            self.assertEqual(expected, actual, 'Update UI with new key.')

    def test_paginated_resource(self):
        #save 40 dummy video objects
        for i in range(40):
            vid = {'path': 's3://apollo-source-data/inputs/obj_det_vid/holo clip.mp4', 'detection_class': 'person', 'detection_score': 98.65, 'seconds': [(6, 12)]}
            self.database.save_record_to_database(vid, VideoDetections)

        with self.flask_app.app_context():
            from api import db
            from api.search import PaginatedResource
            query_all = db.session.query(VideoDetections)

            args = {'page': 0}
            resource = PaginatedResource()
            paginated_results, num_pages = resource.paginate_results(args, query_all)
            self.assertEqual(30, len(paginated_results.all()))
            self.assertEqual(2, num_pages)

            args = {'page': 1}
            paginated_results, num_pages = resource.paginate_results(args, query_all)
            self.assertEqual(10, len(paginated_results.all()))
            self.assertEqual(2, num_pages)

            args = {'page': 0, 'items_per_page': 12}  
            paginated_results, num_pages = resource.paginate_results(args, query_all)
            self.assertEqual(12, len(paginated_results.all()))
            self.assertEqual(4, num_pages)

            args = {'page': 3, 'items_per_page': 12}  
            paginated_results, num_pages = resource.paginate_results(args, query_all)
            self.assertEqual(4, len(paginated_results.all()))
            self.assertEqual(4, num_pages)

            #return all results
            args = {}
            paginated_results, num_pages = resource.paginate_results(args, query_all)
            self.assertEqual(40, len(paginated_results.all()))
            self.assertIsNone(num_pages)