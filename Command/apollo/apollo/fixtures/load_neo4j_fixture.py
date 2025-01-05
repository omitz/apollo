import os
import json

from py2neo import Graph, Node, Relationship, NodeMatcher

from apollo import S3FileStore


def load_neo4j_fixture():
    """
    Download neo4j file dump from s3 and load into clean neo4j database
    """

    filestore = S3FileStore()
    filestore.download_file('data_fixtures/neo4j_dump.txt', './')

    host = os.getenv('NEO4J_HOST')
    user, password = os.getenv('NEO4J_AUTH').split('/')
    graph = Graph('bolt://' + host + ':7687', user=user, password=password)
    
    with open('neo4j_dump.txt') as f:
        lines = f.readlines()
        all_labels = ""
        for line in lines:
            vals = line.split('\t')

            if vals[0] == 'NODE':
                #get labels
                labels = vals[2].split(':')[1:]

                #get props
                props = {}
                for i in range(3, len(vals)):
                    if vals[i] != '\n':
                        k, v = vals[i].split(':', 1)
                        props[k] = v
                
                #assign old id as prop
                props['id'] = int(vals[1])

                node = Node(*labels, **props)
                graph.create(node)
                created = graph.exists(node)
                print(f"node {node} successfully created: {created}")

            elif vals[0] == 'RELATIONSHIP':
                node1_id = vals[1]
                node2_id = vals[2]
                name = vals[3]
                
                props = {}
                for i in range(4, len(vals)):
                    if vals[i] != '\n':
                        k, v = vals[i].split(':', 1)
                        props[k] = v

                node_matcher = NodeMatcher(graph)                
                node1 = node_matcher.match().where(f"_.id={node1_id}").first()
                node2 = node_matcher.match().where(f"_.id={node2_id}").first()
                r_type = Relationship.type(name)
                relationship = r_type(node1, node2, **props)
                graph.create(relationship)
                created = graph.exists(relationship)
                print(f"relationship {relationship} successfully created: {created}")
        f.close() 

if __name__ == "__main__":
    load_neo4j_fixture()