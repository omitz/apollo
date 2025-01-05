import os
import requests
import math

import boto3
import numpy as np
from sqlalchemy import desc
from flask import Blueprint, Flask, current_app, jsonify, request
from flask_restful import Resource, Api, reqparse, abort
from flask_praetorian import auth_required
from py2neo import Graph
from py2neo.matching import *

from apollo.models import DetectedFace, DetectedObj, VideoFaceDetections, VideoDetections, Landmark, ClassifyScene, SearchFullText, Speaker, AnalyzeSentiment
from .  import db


search_blueprint = Blueprint('search', __name__)
api = Api(search_blueprint)
RESULTS_PER_PAGE = 30


def get_all_analytic_results(s3_filename):
    """
    Look up results from databases of every analytic for a given file
    Maybe there's a more extensible way to do this than to check every db table

    Get all results as a list instead of each individually for faster?
    """
    all_results = {}
    
    ###IMAGE RESULTS
    #facial recognition results
    facial_recognition_results = [face_result.serialize() for face_result in db.session.query(DetectedFace).filter_by(original_source=s3_filename).distinct(DetectedFace.prediction)]
    all_results['facial_recognition'] = facial_recognition_results

    #object detection (image) results
    object_detection_results = [object_result.serialize() for object_result in db.session.query(DetectedObj).filter_by(original_source=s3_filename).distinct(DetectedObj.detection_class)]
    all_results['object_detection'] = object_detection_results

    #scene classification results
    scene_classification_results = db.session.query(ClassifyScene).filter_by(original_source=s3_filename).first()
    if scene_classification_results:
        scene_classification_results = scene_classification_results.serialize()
    all_results['scene_classification'] = scene_classification_results

    #OCR results
    #Is there a way to check which would be the best?
    ocr_services = ['ocr_easy', 'ocr_keras', 'ocr_tesseract']
    ocr_results = [ocr_result.serialize("") for ocr_result in db.session.query(SearchFullText).filter_by(original_source=s3_filename).filter(SearchFullText.service_name.in_(ocr_services))]
    all_results['ocr'] = ocr_results

    #Landmark would not be meaningful to include here

    ###AUDIO RESULTS
    #speaker recognition results
    speaker_recognition_results = db.session.query(Speaker).filter_by(original_source=s3_filename).order_by(desc(Speaker.score)).first()
    if speaker_recognition_results:
        speaker_recognition_results = speaker_recognition_results.serialize()
    all_results['speaker_recognition'] = speaker_recognition_results

    #speech to text results
    STT_services = ['speech_to_text']
    STT_results = [STT_result.serialize("") for STT_result in db.session.query(SearchFullText).filter_by(original_source=s3_filename).filter(SearchFullText.service_name.in_(STT_services))]
    all_results['speech_to_text'] = STT_results

    ###VIDEO RESULTS
    #Face detection (video)
    facial_recognition_video_results = [face_result.serialize() for face_result in db.session.query(VideoFaceDetections).filter_by(original_source=s3_filename).distinct(VideoFaceDetections.prediction)]
    all_results['video_facial_recognition'] = facial_recognition_video_results
    
    #object detection (video) results
    object_detection_video_results = [object_result.serialize() for object_result in db.session.query(VideoDetections).filter_by(original_source=s3_filename).distinct(VideoDetections.detection_class)]
    all_results['video_object_detection'] = object_detection_video_results

    ###TEXT FILE RESULTS
    #NER results:
    host = os.getenv('NEO4J_HOST')
    user, password = os.getenv('NEO4J_AUTH').split('/')
    graph = Graph(host=host, user=user, password=password)
    cursor = graph.run("MATCH (a:DOCUMENT {original_source:'" + s3_filename + "'}) OPTIONAL MATCH (a)-[r]-(b) RETURN *;")
    
    ner_results = []
    for record in cursor:
        relationship = record[2] #format node1, node2, relationship for each entry
        start_node = record[1]
        if relationship is not None and start_node is not None:
            ner_results.append({"relationship_type": type(relationship).__name__, "entity": start_node['name']})
    cursor.close()
    all_results['named_entity_recognition_results'] = ner_results

    # Sentiment analysis results
    sentiment_results = [sentiment_result.serialize() for sentiment_result in db.session.query(AnalyzeSentiment).filter_by(original_source=s3_filename)]
    all_results['sentiment_results'] = sentiment_results

    return all_results

def add_aggregated_results(result_list, key):

    """ 
    takes a list of results, ie. a list of DetectedObj
    and returns the list with aggregated analytic results added in

    Format for each item will look like: 
    {
        key: result_object,
        'analytic_results': {
            'facial_recognition': [...]
            ...
        }
    }
    """

    aggregated_results = []

    for result in result_list:
        
        if type(result) is dict:
            json_serialized_object = result
            analytic_results = get_all_analytic_results(result['original_source']) if 'original_source' in result and result['original_source'] else get_all_analytic_results(result['path'])
        else:
            json_serialized_object = result.serialize()
            analytic_results = get_all_analytic_results(result.original_source) if result.original_source else get_all_analytic_results(result.path)
        
        aggregated_results.append({
            key: json_serialized_object,
            "analytic_results": analytic_results
        })

    return aggregated_results


def cmp_to_key(mycmp, extraParm):
    'Convert a cmp= function into a key= function'
    class K:
        def __init__(self, obj, *args):
            self.obj = obj
        def __lt__(self, other):
            return mycmp(self.obj, other.obj, extraParm) < 0
        def __gt__(self, other):
            return mycmp(self.obj, other.obj, extraParm) > 0
        def __eq__(self, other):
            return mycmp(self.obj, other.obj, extraParm) == 0
        def __le__(self, other):
            return mycmp(self.obj, other.obj, extraParm) <= 0
        def __ge__(self, other):
            return mycmp(self.obj, other.obj, extraParm) >= 0
        def __ne__(self, other):
            return mycmp(self.obj, other.obj, extraParm) != 0
    return K


class AnalyticResultsResource(Resource):

    @auth_required
    def get(self):
        _file = request.args['file']
        s3_path = _file
        if not s3_path.startswith("s3://" + current_app.config['BUCKET_NAME']):
            s3_path = "s3://" + os.path.join(current_app.config['BUCKET_NAME'], _file)

        print(f"s3_path: {s3_path}", flush=True)
        return jsonify(get_all_analytic_results(s3_path))


class PaginatedResource(Resource):

    def parse_args(self, args):
        page_number = args['page'] if 'page' in args else None
        items_per_page = args['items_per_page'] if 'items_per_page' in args else None
        return page_number, items_per_page

    def paginate_results(self, args, query_objects):
        '''
        :param args: ImmutableMultiDict;
                    eg ImmutableMultiDict([('tag', 'tie'), ('page', '1'), ('items_per_page', '2')])
        :param query_objects: flask_sqlalchemy BaseQuery
        '''
        page_number, items_per_page = self.parse_args(args)
        paginated_objects = query_objects
        num_pages = None
        if page_number is not None:
            if items_per_page:
                num_pages = math.ceil(query_objects.count() / int(items_per_page))
                paginated_objects = query_objects.limit(int(items_per_page)).offset(int(page_number) * int(items_per_page))
            else:
                num_pages = math.ceil(query_objects.count() / RESULTS_PER_PAGE)
                paginated_objects = query_objects.limit(RESULTS_PER_PAGE).offset(int(page_number) * RESULTS_PER_PAGE)
        
        return paginated_objects, num_pages

    def paginate_results_list(self, args, query_objects):
        '''
        Paginate a list of results (rather than a BaseQuery).
        :param args: ImmutableMultiDict;
                    eg ImmutableMultiDict([('tag', 'tie'), ('page', '1'), ('items_per_page', '2')])
        :param query_objects: List
        '''
        page_number, items_per_page = self.parse_args(args)
        paginated_objects = query_objects
        num_pages = None
        if page_number is not None:
            if items_per_page:
                items_per_page = int(items_per_page)
                num_pages = math.ceil(len(query_objects) / items_per_page)
                start = int(page_number) * items_per_page
                end = start + items_per_page
            else:
                num_pages = math.ceil(len(query_objects) / RESULTS_PER_PAGE)
                start = int(page_number) * RESULTS_PER_PAGE
                end = start + RESULTS_PER_PAGE
            indices = range(start, end)
            paginated_objects = [query_objects[i] for i in indices if i < len(query_objects)]
        return paginated_objects, num_pages

class SearchByTag(PaginatedResource):

    # http://localhost:8080/search/tag/?tag=dog
    @auth_required
    def get(self):
        search_tag = request.args[self.get_query_param()]
        
        print(f'SearchByTag: {search_tag}', flush=True)

        #get the detected object result id for each unique s3 path with the highest detection score
        unique_path_objects_ids = list(map(lambda obj: obj.id, db.session.query(DetectedObj)                                                                            .filter_by(detection_class=search_tag)
                                                                            .distinct(DetectedObj.path)
                                                                            .all()
                                        ))
        print(unique_path_objects_ids, flush=True)
        if len(unique_path_objects_ids) == 0:
            classes_in_table = set(row.detection_class for row in db.session.query(DetectedObj).all())
            if len(classes_in_table) == 0:
                return jsonify(f'The DetectedObj table is currently empty.')
            return jsonify(
                f'The label {search_tag} was not found in the DetectedObj table. Labels currently in the DetectedObj table are {classes_in_table}.')
        
        query_objects = db.session.query(DetectedObj) \
                                    .filter(DetectedObj.id.in_(unique_path_objects_ids)) \
                                    .order_by(desc(DetectedObj.detection_scores))

        paginated_objects, num_pages = self.paginate_results(request.args, query_objects)

        objects = { self.get_json_key(): list(map(lambda obj_det: obj_det.serialize(), paginated_objects)), 'num_pages': num_pages}
                   
        #objects[self.get_json_key()] = add_aggregated_results(objects[self.get_json_key()], "object_detection")
        return jsonify(objects)

    def get_query_param(self):
        return 'tag'

    def get_json_key(self):
        return 'objects'

class SearchByTagVideo(PaginatedResource):
    # http://localhost:8080/search/tag_video/?tag=person

    @auth_required
    def get(self):
        search_tag = request.args[self.get_query_param()]
        query_result = list(map(lambda obj_det: obj_det.serialize(), db.session.query(VideoDetections).filter_by(detection_class=search_tag).all()))
        # get the detected object result id for each unique s3 path with the highest detection score
        unique_path_objects_ids = list(map(lambda obj: obj.id, db.session.query(VideoDetections)
                                           .filter_by(detection_class=search_tag)
                                           .order_by(VideoDetections.path, desc(VideoDetections.detection_score))
                                           .distinct(VideoDetections.path)
                                           .all()
                                           ))
        if len(unique_path_objects_ids) == 0:
            classes_in_table = set(row.detection_class for row in db.session.query(VideoDetections).all())
            if len(classes_in_table) == 0:
                return jsonify(f'The VideoDetections table is currently empty.')
            return jsonify(f'The label {search_tag} was not found in the VideoDetections table. Labels currently in the VideoDetections table are {classes_in_table}.')
       
        query_results = db.session.query(VideoDetections) \
                            .filter(VideoDetections.id.in_(unique_path_objects_ids)) \
                            .order_by(desc(VideoDetections.detection_score))
                            
        paginated_results, num_pages = self.paginate_results(request.args, query_results)

        objects = {self.get_json_key(): [ obj_det.serialize() for obj_det in paginated_results ], 'num_pages': num_pages}

        #objects[self.get_json_key()] = add_aggregated_results(objects[self.get_json_key()], "object_detection")       
        
        return jsonify(objects)

    def get_query_param(self):
        return 'tag'

    def get_json_key(self):
        return 'objects'

class GetSignedUrl(Resource):

    @auth_required
    def get(self):
        print('In GetSignedUrl', flush=True)
        bucket = current_app.config['BUCKET_NAME']
        key = request.args['key']
        print(f'GetSignedUrl: {bucket}, {key}', flush=True)
        s3_client = boto3.client('s3')
        n_seconds = 60*5
        url = s3_client.generate_presigned_url(
            ClientMethod='get_object',
            Params={
                'Bucket': bucket,
                'Key': key
            },
            ExpiresIn=n_seconds
        )
        return jsonify(url)


class SearchBySceneHierarchy(PaginatedResource):

    @auth_required
    def get(self):
        """
        hierarchy = "indoor" or "outdoor"
        """
        search_tag = request.args[self.get_query_param()]
        print(f'SearchBySceneClassTag: Enter Get Scene by Hierarchy {search_tag}', flush=True)

        # search_tag must be either "outdoor" or "indoor"
        if (not search_tag in ["outdoor", "indoor"]):
            print(f"ERROR!! invalid scene hierarchy: {search_tag}")
            print(f"ERROR!! must be 'outdoor' or 'indoor'")
            return jsonify({'scenes': []}) # return empty list

        query = db.session.query(ClassifyScene) \
                          .filter_by(class_hierarchy=search_tag)
        paginated_results, num_pages = self.paginate_results(request.args, query)

        results = list(map(lambda scene_class: scene_class.serialize(), paginated_results))

        return jsonify({'scenes': results, 'num_pages': num_pages})

    def get_query_param(self):
        return 'tag'

    
class SearchBySceneClassTag(PaginatedResource):
    @staticmethod
    def compareTop5s (elm1, elm2, search_term):
        elm1_top5 = elm1['top_five_classes']
        elm2_top5 = elm2['top_five_classes']

        elm1_rank = elm1_top5.index (search_term)
        elm2_rank = elm2_top5.index (search_term)

        if elm1_rank < elm2_rank:
            return -1
        if elm1_rank > elm2_rank:
            return 1
        return 0

    @auth_required
    def get(self):
        """
        Examples of scene class: "restaurant", "bedroom", "mountain", "parking lot", etc.
        See ClassifyScene.classes
        """
        search_tag = request.args[self.get_query_param()]
        print(f'SearchBySceneClassTag: Enter Get Scene by Class {search_tag}', flush=True)

        ## convert search tag to list index
        try:
            search_idx = ClassifyScene.classes.index(search_tag)
        except:
            print(f"ERROR!! invalid scene class: {search_tag}")
            return jsonify({'scenes': []}) # return empty list

        results = list (map(lambda scene_class: scene_class.serialize(),
                            db.session.query(ClassifyScene).filter(
                                ClassifyScene.top_five_classes.any(search_idx)).all()))

        ## we should sort by top5 rank first and then paginate afterward.
        ## Sort the result by top rank.  'result' is a list of dictionary
        results_sorted = sorted (results, key=cmp_to_key(self.compareTop5s, search_tag))

        paginate_results, num_pages = self.paginate_results_list(request.args, results_sorted)
        return jsonify({'scenes': paginate_results, 'num_pages': num_pages})

    def get_query_param(self):
        return 'tag'

class SearchByEntity(Resource):
    # http://localhost:8080/search/ner/ # 'search' as defined in Blueprint
    
    @auth_required
    def get(self):
        search_entity = request.args[self.get_query_param()]
        payload = {'entity': search_entity}
        ner_search_host = current_app.config['NER_SEARCH_HOST'] # When running locally, this should be 'named-entity-recognition-search'. 'named-entity-recognition-search' is the service name in docker-compose and thus is also the IP address for the ner search container
        url = 'http://' + ner_search_host + ':84/search/'
        print(f'url: {url}\npayload: {payload}', flush=True)
        
        if 'MOCK_SEARCH_SERVICES' in current_app.config and current_app.config['MOCK_SEARCH_SERVICES']:
            return {'ner_data': []}
        
        response = requests.get(url, params=payload)
        print(f'response: {response}\nresponse type: {type(response)}', flush=True)
        
        try:
            json_response = response.json()

            print(f"json {json_response}", flush=True)
               
            #json_response['ner_data'] = add_aggregated_results(json_response["ner_data"], "ner")
            return jsonify(json_response)

        except:
            return response

    def get_query_param(self):
        return 'entity'

    
class SearchInFullText(PaginatedResource):

    @auth_required
    def get(self):
        """
        Query example:

        session.query(Model).filter (Model.tsvector.op('@@')(func.plainto_tsquery('search string')))
        people = Person.query.filter (Person.__ts_vector__.match(expressions,
                                  postgresql_regconfig='english')).all()

        https://www.postgresql.org/docs/9.5/textsearch-controls.html#TEXTSEARCH-PARSING-QUERIES
        See https://www.postgresql.org/docs/9.5/datatype-textsearch.html

        """
        #querytext must include of single tokens separated by the
        #Boolean operators & (AND), | (OR) and ! (NOT). These
        #operators can be grouped using parentheses.  To convert to
        #http query string, use https://www.url-encode-decode.com/
        querytext = request.args[self.get_query_param()]

        print(f'SearchInFullText: Enter Search in Full Text "{querytext}"', flush=True)

        results = db.session.query(SearchFullText).filter(
                                SearchFullText.search_vector.match(
                                    querytext,
                                    postgresql_regconfig='english'))

        paginated_results, num_pages = self.paginate_results(request.args, results)
        #results = add_aggregated_results(results, "fulltext", querytext)

        return jsonify({'fulltexts': [fulltext_result.serialize(querytext) for fulltext_result in paginated_results], 'num_pages': num_pages})

    def get_query_param(self):
        return 'query'



class SearchBySentiment(PaginatedResource):


    @staticmethod
    def comparePolarities (elm1, elm2, sentiment):
        elm1_polarity = elm1.polarity
        elm2_polarity = elm2.polarity

        if sentiment == "negative":
            # show the lower polarity ones first
            if elm1_polarity > elm2_polarity:
                return 1
            if elm1_polarity < elm2_polarity:
                return -1
        elif sentiment == "positive":
            # show the higher polarity ones first
            if elm1_polarity < elm2_polarity:
                return 1
            if elm1_polarity > elm2_polarity:
                return -1
        return 0

    
    @auth_required
    def get(self):
        """
        """
        query_sentiment = request.args[self.get_query_param()]

        print(f'SearchBySentiment: Enter Search by Sentiment "{query_sentiment}"', flush=True)

        # search_tag must be either "positive" or "negative" or "neutral"
        if (not query_sentiment in ["positive", "neutral", "negative"]):
            print(f"ERROR!! invalid sentiment query: {query_sentiment}")
            print(f"ERROR!! must be 'positive' 'neutral', or 'negative'")
            return jsonify({'sentiments': []}) # return empty list

        results = db.session.query(AnalyzeSentiment).filter_by(sentiment=query_sentiment)
        results_sorted = sorted (results, key=cmp_to_key(self.comparePolarities, query_sentiment))
        paginated_results, num_pages = self.paginate_results_list(request.args, results_sorted)

        results_dict = {'sentiments': [result.serialize()
                                       for result in paginated_results],
                        'num_pages': num_pages}
        return jsonify(results_dict)
        
    def get_query_param(self):
        return 'sentiment'
    

class CheckDataBase(Resource):

    @auth_required
    def get(self):
        """
        url parameters:
          'model':  Name of the SqlAlchemy model, (eg., 'SearchFullText') See models.py
          'path':  [optional] The s3 path, (eg., 's3://apollo-source-data...')

        Return Number of records or True if model has a record corresponding to 
        
        """

        ## Get table name
        if ('model' not in request.args):
            print ("Need model name in ", request.args, flush=True)
            print ("Example: \n" +
                   "\t path='s3://apollo-source-data/input/audio/bill_gates-TED.mp3'\n" +
                   "\t model='SearchFullText'",  flush=True)
            print ("request.args = ", request.args, flush=True)
            return jsonify ("need 'model' paramter. 'path' is optional. ")
            
        modelName = request.args['model']
        try:
            model = eval(modelName) # may crash
            print ("model = ", model, flush=True)            
        except:
            print (f"Invalid model '{modelName}'", flush=True)
            return jsonify (f"Invalid model '{modelName}'")

        
        ## Get file path if provide
        if ('path' in request.args):
            s3filepath = request.args['path']
            query = db.session.query(model).filter (s3filepath == model.path)
            query_res = query.all()
            if len(query_res) == 0:
                return jsonify (False)
            else:
                return jsonify (True)

        ## return number of data in the database
        nData = db.session.query(model).count()
        return jsonify (nData)

api.add_resource(SearchByTag, '/tag/')
api.add_resource(SearchByTagVideo, '/tag_video/')
api.add_resource(SearchBySceneHierarchy, '/scene_hierarchy/') # eg. 'indoor'
api.add_resource(SearchBySceneClassTag, '/scene_class/')  # eg. 'food court'
api.add_resource(SearchByEntity, '/ner/')  # eg. 'Lebanon'
api.add_resource(SearchInFullText, '/full_text/')  # eg. 'epidemic', 'epidemic+%7C+indian'
api.add_resource(SearchBySentiment, '/text_sentiment/')  # eg. 'positive', 'negative', 'neutral'
api.add_resource(GetSignedUrl, '/get_signed_url/')
api.add_resource(CheckDataBase, '/check_database/')
api.add_resource(AnalyticResultsResource, '/results/')
