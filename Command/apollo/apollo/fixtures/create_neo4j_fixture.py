import os
import json

from py2neo import Graph
from py2neo.matching import *

from apollo import S3FileStore


def create_neo4j_fixture():
    """
    Create a file representation neo4j database and save to s3
    """

    host = os.getenv('NEO4J_HOST')
    user, password = os.getenv('NEO4J_AUTH').split('/')
    
    graph = Graph(host=host, user=user, password=password)
    #results = graph.match(None, None)
    nodes = NodeMatcher(graph).match()
    relationships = RelationshipMatcher(graph).match()
    
    with open('neo4j_dump.txt', 'w') as f:
        for node in nodes.__iter__():
            node_str = f"NODE\t{node.identity}\t{node.labels}\t"
            for item in node.items():
                node_str = node_str + f"{item[0]}:{item[1]}\t"
            node_str = node_str + "\n"
            print(node_str)
            f.write(node_str)

        for relationship in relationships.__iter__():
            rel_str = f"RELATIONSHIP\t{relationship.start_node.identity}\t{relationship.end_node.identity}\t{relationship.__class__.__name__}\t"
            for item in relationship.items():
                rel_str = rel_str + f"{item[0]}:{item[1]}\t"
            rel_str = rel_str + "\n"
            print(rel_str)
            f.write(rel_str)
        f.close()
   
    filestore = S3FileStore()
    filestore.upload_file('neo4j_dump.txt', 'data_fixtures/neo4j_dump.txt')     

if __name__ == "__main__":
    create_neo4j_fixture()