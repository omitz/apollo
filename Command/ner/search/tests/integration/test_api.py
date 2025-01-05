import unittest
import os
import pandas as pd
import re
import time
from commandutils import neo4j_utils as nu
from search.api import create_app
from search.api.search import SearchByEntity, get_documents

class TestSearch(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        os.environ['NEO4J_HOST'] = 'neo4j'
        os.environ['NEO4J_AUTH'] = 'neo4j/neo4j-password'
        nu.wait_for_neo4j()
        cls.flask_app = create_app('search.config.Config')
        # Drop any existing local neo4j graph data
        nu.delete_all_from_graph()

        cls.graph = nu.get_graph()

    @classmethod
    def tearDown(self):
        # Drop any existing local neo4j graph data
        nu.delete_all_from_graph()

    @unittest.skipIf(os.getenv('JENKINS'), "Test does not work in jenkins bc `get` downloads a file from S3.")
    def test_SearchByEntity_get_multiple_occurrences(self):
        '''
        Test that `get` returns a snippet which includes both occurences of the search entity.
        '''
        # Build up some data in the neo4j graph
        # Mock an entity that gets detected in a document
        data = {'text': ['Hassan Nasrallah'], 'label': ['PERSON'], 'is_vip': [True], 'snippets': ['some sample text']}
        df = pd.DataFrame.from_dict(data)
        s3_path = 's3://apollo-source-data/inputs/ner/test_mentioned_twice.txt'
        nu.save_dataframe_to_graph(self.graph, df, s3_path)

        search_entity = 'Hassan Nasrallah'
        formatted = search_entity.replace(' ', '+')
        query = f'/search/?entity={formatted}'
        with self.flask_app.test_client() as client:
            response = client.get(query)
        json_response = response.json
        relevant_entry = [d for d in json_response['ner_data'] if d['path'] == s3_path][0]
        indices_of_search_term = [m.start() for m in re.finditer(search_entity, relevant_entry['snippet'])]
        self.assertEqual(len(indices_of_search_term), 2)

    @unittest.skipIf(os.getenv('JENKINS'), "Test does not work in jenkins bc `get` downloads a file from S3.")
    def test_SearchByEntity_get_not_in_graph(self):
        # Build up some data in the neo4j graph
        # Mock an entity that gets detected in a document
        data = {'text': ['Hassan Nasrallah'], 'label': ['PERSON'], 'is_vip': [True], 'snippets': ['some sample text']}
        df = pd.DataFrame.from_dict(data)
        s3_path = 's3://apollo-source-data/inputs/ner/test_mentioned_twice.txt'
        nu.save_dataframe_to_graph(self.graph, df, s3_path)

        search_entity = 'Some Person'
        formatted = search_entity.replace(' ', '+')
        query = f'/search/?entity={formatted}'
        with self.flask_app.test_client() as client:
            response = client.get(query)
        json_response = response.json
        res = json_response['ner_data']
        # We expect an empty list
        self.assertTrue(len(res) == 0)

    def test_get_documents(self):
        '''
        Test the cypher query in get_documents - more specifically that it will only return documents connected to the query
        '''
        # Build up some data in the neo4j graph
        # Mock an entity that gets detected in a document
        data = {'text': ['Hassan Nasrallah'], 'label': ['PERSON'], 'is_vip': [True], 'snippets': ['some sample text']}
        df = pd.DataFrame.from_dict(data)
        s3_path = 's3://apollo-source-data/inputs/ner/test.txt'
        nu.save_dataframe_to_graph(self.graph, df, s3_path)
        # Mock entities that gets detected in another document
        search_entity = 'China'
        data2 = {'text': ['Hassan Nasrallah', search_entity], 'label': ['PERSON', 'LOC'], 'is_vip': [True, False], 'snippets': ['some sample text', None]}
        df2 = pd.DataFrame.from_dict(data2)
        s3_path2 = 's3://apollo-source-data/inputs/ner/test_mentioned_twice.txt'
        nu.save_dataframe_to_graph(self.graph, df2, s3_path2)

        docs = get_documents(self.graph, 'China')
        self.assertListEqual([{'name':s3_path2, 'original_source': 's3://apollo-source-data/inputs/ner/test_mentioned_twice.txt'}], docs)
