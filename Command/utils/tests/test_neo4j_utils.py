from unittest import TestCase
import pandas as pd
import docker, os
import time
from commandutils.neo4j_utils import save_dataframe_to_graph, get_graph, wait_for_neo4j


class TestNeo4jUtils(TestCase):
    @classmethod
    def setUpClass(cls):
        time.sleep(10) #neo4j takes a bit to get up
        os.environ['NEO4J_AUTH'] = 'neo4j/neo4j-password'
        os.environ['NEO4J_HOST'] = 'neo4j'

        wait_for_neo4j()

        # Clear the existing graph (if there is one)
        cls.graph = get_graph()
        # Delete all relationships
        cls.graph.run('match ()-[r]->() delete r')
        # Delete all nodes
        cls.graph.run('match (n) delete (n)')

    @classmethod
    def tearDownClass(cls):
        cls.graph.run('match ()-[r]->() delete r')
        # Delete all nodes
        cls.graph.run('match (n) delete (n)')

    def test_save_dataframe_to_graph(self):
        '''
        Test that save_dataframe_to_graph creates the expected nodes and relationships - more specifically, that it joins the graphs of two separate "docs" that both mention the same person.
        '''
        person = 'Hassan Nasrallah'
        # Create data for 1 doc
        data1 = {'label': ['PERSON', 'PLACE'],
                'text': [person, 'Lebanon'],
                'is_vip': [True, False],
                'snippets': ['', '']}
        df1 = pd.DataFrame.from_dict(data1)

        # Add it to the graph
        save_dataframe_to_graph(self.graph, df1, 'some_s3_file.pdf')
        # Create data for a second doc
        data2 = {'label': ['PERSON', 'PLACE'],
                'text': [person, 'Turkey'],
                'is_vip': [True, False],
                'snippets': ['', '']}
        df2 = pd.DataFrame.from_dict(data2)
        save_dataframe_to_graph(self.graph, df2, 'some_other_s3_file.txt')
        # Run cypher queries
        nodes_table = self.graph.run('match (n) return n').to_table()
        relationships_table = self.graph.run('match ()-[r]->() return r;').to_table()
        # Select all places with any relationship to a document with any relationship to a person where the person's name is Hassan Nasrallah
        places_connected_to_hn = self.graph.run(f'match (n:PLACE)-[*]-(DOCUMENT)-[*]-(p:PERSON) where p.name = \'{person}\' return n;').to_table()
        places_list = [t[0]['name'] for t in places_connected_to_hn]
        self.assertEqual(len(nodes_table), 5)
        self.assertEqual(len(relationships_table), 4)
        self.assertListEqual(places_list, ['Lebanon', 'Turkey'])