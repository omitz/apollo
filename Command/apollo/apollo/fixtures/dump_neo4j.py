import os

from py2neo import Graph


def dump_neo4j():
    """
    Delete all data in neo4j database
    """

    host = os.getenv('NEO4J_HOST')
    user, password = os.getenv('NEO4J_AUTH').split('/')
    graph = Graph('bolt://' + host + ':7687', user=user, password=password)

    graph.run('MATCH (n) DETACH DELETE n')

if __name__ == "__main__":
    dump_neo4j()