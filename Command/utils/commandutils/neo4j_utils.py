import os

from py2neo import Graph, NodeMatcher
from py2neo import Node, Relationship
from neobolt.exceptions import ServiceUnavailable

def wait_for_neo4j():
    host = os.getenv('NEO4J_HOST')
    user, password = os.getenv('NEO4J_AUTH').split('/')
    graph = Graph('bolt://' + host + ':7687', user=user, password=password)
    
    ready = False
    while not ready:
        try:
            graph.run("Match () Return 1 Limit 1")
            ready = True
        except (ConnectionRefusedError, ServiceUnavailable) as ex:
            ready = False

def get_graph():
    host = os.getenv('NEO4J_HOST')
    user, password = os.getenv('NEO4J_AUTH').split('/')
    return Graph('bolt://' + host + ':7687', user=user, password=password)

def save_dataframe_to_graph(graph, df, s3_filename, original_source=None):
    '''
    Args:
        df: A pandas dataframe where each row represents one entity. The dataframe should have the following columns: label, text, vip, and snippets.
    '''
    # Make a node for the document itself
    if original_source is None:
        original_source = s3_filename
        
    doc_label = 'DOCUMENT'
    doc_node = Node(doc_label, name=s3_filename, original_source=original_source)
    graph.merge(doc_node, doc_label, 'name')
    # Connect the entities in the document to the doc node
    for row in df.itertuples(index=False):
        if row.is_vip:
            node = Node(row.label, name=row.text, vip=True, original_source=original_source)
            if row.snippets and len(row.snippets) > 0: # If there was any threatening language
                relationship_type = 'THREAT_IN'
                relationship = Relationship(node, relationship_type, doc_node, snippets=row.snippets.replace('\n', ''))
                graph.merge(node, row.label, 'name')
                graph.merge(relationship, relationship_type, 'name')
        else:
            node = Node(row.label, name=row.text, original_source=original_source)
        relationship_type = 'MENTIONED_IN'
        relationship = Relationship(node, relationship_type, doc_node)
        graph.merge(node, row.label, 'name')
        graph.merge(relationship, relationship_type, 'name')

        # # Previous graph structure. Leaving here for reference
        # # You have to pass the node to graph create (or graph merge) to actually send the REST API call to create it
        # for previous_node in nodes:
        #     # There is a way to assign all nodes bidirectional relationships, but the recommended practice is to make a unidirectional relationship, and then when you query, ignore relationship direction
        #     relationship = Relationship(previous_node, 'MENTIONED_WITH', node, source=s3_filename)  # The first arg is the 'from' node # 'source' here is a new relationship property of my choosing (ie I could've done source_article=data.csv)
        #     graph.merge(relationship, 'MENTIONED_WITH', 'source')
        # nodes.append(node)


def get_all_from_graph():
    graph = get_graph()
    return graph.run('match (n) return n').to_table()


def delete_all_from_graph():
    graph = get_graph()
    graph.delete_all()

