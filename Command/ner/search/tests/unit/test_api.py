import unittest
import os
import pandas as pd
import re
import time
from commandutils import neo4j_utils as nu
from search.api import create_app
from search.api.search import get_text_surrounding_entity

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

    def test_get_text_surrounding_entity(self):
        '''
        Test that `get` returns a snippet which includes both occurences of the search entity. Avoid S3 download so this can run in Jenkins.
        '''
        # Build up some data in the neo4j graph
        # Mock an entity that gets detected in a document
        data = {'text': ['Hassan Nasrallah'], 'label': ['PERSON'], 'is_vip': [True], 'snippets': ['some sample text']}
        df = pd.DataFrame.from_dict(data)
        s3_path = 's3://apollo-source-data/inputs/ner/test_mentioned_twice.txt'
        nu.save_dataframe_to_graph(self.graph, df, s3_path)

        doc_path = 'ner_spacy/tests/test_files/test_mentioned_twice.txt'
        search_entity = 'Hassan Nasrallah'

        snippets = get_text_surrounding_entity(doc_path, search_entity)
        indices_of_search_term = [m.start() for m in re.finditer(search_entity, snippets)]
        self.assertEqual(len(indices_of_search_term), 2)
