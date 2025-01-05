from flask import Blueprint,jsonify, request
from flask_restful import Api, Resource
import os
import numpy as np
import magic
import re
from commandutils import s3_utils, neo4j_utils, file_utils
from ner_spacy.ner_utils import load_and_clean_doc, load_with_lang_model, concat_text


search_blueprint = Blueprint('search', __name__)
search_api = Api(search_blueprint)

class HealthCheckResource(Resource):
    def get(self):
        return {'hello': 'world'}

class SearchByEntity(Resource):
    # curl -X GET localhost:8084/search/?entity=Lebanon
    # Replace spaces in entity with a plus symbol, e.g. curl -X GET localhost:8084/search/?entity=Hassan+Nasrallah
    def get(self):
        search_entity = request.args['entity']
        print(f'search entity: {search_entity}')
        graph = neo4j_utils.get_graph()

        documents = get_documents(graph, search_entity)

        s3paths_and_snippets = []
        # String together the words and punctuation surrounding the entity
        fm = file_utils.FileManager()
        for doc in documents:
            doc_res = {}
            # Download it from S3
            s3, target = s3_utils.access_bucket_and_download(doc['name'], fm.ram_storage)
            text_file = os.path.split(target)[1]
            doc_path = os.path.join(fm.ram_storage, text_file)
            # Note: UI relies on 'path' key
            doc_res['path'] = doc['name']
            doc_res['original_source'] = doc['original_source']
            snippets = get_text_surrounding_entity(doc_path, search_entity)
            # Note: UI relies on 'snippet' key
            doc_res['snippet'] = snippets
            s3paths_and_snippets.append(doc_res)
        print(f'\ns3 paths and snippets: {s3paths_and_snippets}\n', flush=True)
        # Note: UI relies on 'ner_data' key
        return jsonify({'ner_data': s3paths_and_snippets})
        # TODO (low priority) handle case of countries: When entered into the graph, if an alias for a country is found (eg Burma) co converter will change that to Myanmar. If the user queries Burma, they'll get no results. If they query Myanmar, they'll get document results but no string results.


def get_text_surrounding_entity(doc_path, search_entity):
    # Get the mimetype
    print(f'Checking the mimetype', flush=True)
    file_checker = magic.Magic(mime=True)
    mimetype = file_checker.from_file(doc_path)
    # Load the doc
    text = load_and_clean_doc(doc_path, mimetype)
    # Find the search entity in the doc
    start_indices_of_search_entity = [m.start() for m in re.finditer(search_entity, text)]
    padding = 100
    string_indices_to_return = []
    for i in start_indices_of_search_entity:
        to_add = [j for j in range(i - padding, i + len(search_entity) + padding)]
        string_indices_to_return += to_add
    # Remove negatives
    string_indices_to_return = [i for i in string_indices_to_return if i >= 0]
    # Remove indices beyond the length of the text
    string_indices_to_return = [i for i in string_indices_to_return if i < len(text)]
    # Remove duplicates, sort, add ellipses if necessary.
    snippets = concat_text(text, string_indices_to_return, spaces=False)
    return snippets


def get_documents(graph, search_entity):
    '''
    :param graph: A py2neo Graph
    :param search_entity: The entity the user is searching for, eg "Hassan Nasrallah"
    :return: unique: A list of dicts - one dict per file where the entity was detected.
        Eg [
        {'name': 's3://apollo-source-data/inputs/uploads/some_file.txt', 'original_source': 's3://apollo-source-data/inputs/uploads/some_file.txt'},
        {'name': 's3://apollo-source-data/inputs/uploads/some_other_file.txt', 'original_source': 's3://apollo-source-data/inputs/uploads/some_other_file.txt'}
        ]
    '''
    # Find all documents with any relationship to the search_entity
    cypher_query = f'match (d:DOCUMENT)-[r]-(n) where n.name = \'{search_entity}\' return d.name, d.original_source'
    documents = graph.run(cypher_query)

    # Remove duplicates and convert to dicts
    doc_names = []
    unique = []
    for document in documents:
        if not document['d.name'] in doc_names:
            doc_names.append(document['d.name'])
            unique.append(result_to_dict(document))
    print(f"unique documents: {unique}", flush=True)
    return unique


def result_to_dict(result):
    return {"name": result["d.name"], "original_source": result["d.original_source"]}


class GetSize(Resource):
    """ Return size of the graph.
    Useful for checking insertion.
    """
    def get(self):
        return len (neo4j_utils.get_all_from_graph())

search_api.add_resource(HealthCheckResource, '/health')
search_api.add_resource(SearchByEntity, '/search')
search_api.add_resource(GetSize, '/size')
