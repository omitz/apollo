import os
import time

from py2neo import Graph, NodeMatcher
from py2neo import Node, Relationship
from neobolt.exceptions import ServiceUnavailable

class Neo4jGraphDatabase():

    def __init__(self, host=None, user=None, password=None):
        
            """
            connect to database, create databases, create all tables
            """

            if host is None:
                host = os.getenv('NEO4J_HOST')
            if user is None or password is None:
                user = os.getenv('NEO4J_AUTH').split('/')[0]
                password = os.getenv('NEO4J_AUTH').split('/')[1]
            
            tries_left = 10
            ready = False
            while not ready and tries_left:
                try:
                    self.graph = Graph('bolt://' + host + ':7687', user=user, password=password)
                    ready = True
                except (ConnectionRefusedError, ServiceUnavailable) as ex:
                    ready = False
                    tries_left = tries_left - 1
                    print("neo4j not ready, sleeping 5 seconds...")
                    time.sleep(5)
            
            

    def wait_until_available(self):
        
        ready = False
        while not ready:
            try:
                self.graph.run("Match () Return 1 Limit 1")
                ready = True
            except (ConnectionRefusedError, ServiceUnavailable) as ex:
                ready = False