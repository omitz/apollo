from flask import Blueprint, Flask, current_app, jsonify, request
from flask_restful import Resource, Api, reqparse, abort
from flask_praetorian import auth_required

from commandutils import ApolloMessage

import json
import boto3


jobs_blueprint = Blueprint('jobs', __name__)
api = Api(jobs_blueprint)

class PostJobResource(Resource):
    #name of the services that the resources will post a message to
    services = []

    #additional fields that this resource will accept and pass into rabbitmq message
    #see FacialRecognitionResource for example
    additional_fields = []
    
    @auth_required
    def post(self):
        try: 
            path = request.get_json()['path']
        except KeyError:
            raise InvalidUsage('Expected POST body with member "path"')


        if not path.startswith("/"):
            path = "/" + path
            
        messages = []

        for service in self.services:
            message_body = {"description": service, "name": "s3://" + current_app.config['BUCKET_NAME'] + path}

            for field in self.additional_fields:
                if field in request.get_json().keys():
                    message_body[field] = request.get_json()[field]

            apollo_message = ApolloMessage.ApolloMessage(message_body)
            route = service + '_route'

            log_message = f'Sending message to {route}, message: {apollo_message}'
            current_app.logger.info(log_message)

            current_app.rmq_publisher.publish(apollo_message, route)
            messages.append(apollo_message.__dict__)

        return json.dumps(messages)


class FacialRecognitionResource(PostJobResource):
    services = ['facenet', 'face_resnet']
    additional_fields = ['num_milvus_results', 'add_to_db']

class FacialRecognitionVideoResource(PostJobResource):
    services = ['face_vid']

class LandmarkResource(PostJobResource):
    services = ['landmark']

class NerResource(PostJobResource):
    services = ['named_entity_recognition']

class ObjDetResource(PostJobResource):
    services = ['object_detection']

class ObjDetVidResource(PostJobResource):
    services = ['object_detection_vid']

class SpeakerRecognitionResource(PostJobResource):
    services = ['speaker_recognition']

class VirusScannerResource(PostJobResource):
    services = ['virus_scanner']
    additional_fields = ['ignore_hash']

class SceneClassificationResource(PostJobResource):
    services = ['scene_places365']

class SpeechToTextResource(PostJobResource):
    services = ['speech_to_text']

class OcrKerasResource(PostJobResource):
    services = ['ocr_keras']

class OcrTesseractResource(PostJobResource):
    services = ['ocr_tesseract']

class FullTextSearchResource(PostJobResource):
    services = ['full_text_search']

class OcrEasyResource(PostJobResource):
    services = ['ocr_easy']

class SentimentResource(PostJobResource):
    services = ['text_sentiment']
    
class DirectoryResource(Resource):

    @auth_required
    def post(self):
        try: 
            path = request.get_json()['path']
        except KeyError:
            raise InvalidUsage('Expected POST body with member \"path\"')

        if path.startswith("/"):
            path = path[1:]

        s3 = boto3.resource('s3') 
        bucket_name = current_app.config['BUCKET_NAME']
        bucket = s3.Bucket(bucket_name)
        object_summary_iterator = bucket.objects.filter(
            Prefix=path,
        )
           
        for s3_object_summary in object_summary_iterator:
            if not s3_object_summary.key.endswith("/"):

                msg = {'description': 'virus_scanner', 'name': 's3://' + bucket_name + '/' + s3_object_summary.key}
                
                if 'ignore_hash' in request.get_json():
                    msg['ignore_hash'] = request.get_json()['ignore_hash']

                apollo_message = ApolloMessage.ApolloMessage(msg)
                route = 'virus_scanner_route'

                log_message = f'Sending message to {route}, message: {apollo_message}'
                current_app.logger.info(log_message)

                current_app.rmq_publisher.publish(apollo_message, route)

        return {'path': path}

class InvalidUsage(Exception):
    status_code = 400

    def __init__(self, message, status_code=None, payload=None):
        Exception.__init__(self)
        self.message = message
        if status_code is not None:
            self.status_code = status_code
        self.payload = payload

    def to_dict(self):
        rv = dict(self.payload or ())
        rv['message'] = self.message
        return rv

api.add_resource(DirectoryResource, '/')
api.add_resource(FacialRecognitionResource, '/facial_recognition/')
api.add_resource(FacialRecognitionVideoResource, '/face_vid/')
api.add_resource(LandmarkResource, '/landmark/')
api.add_resource(NerResource, '/named_entity_recognition/')
api.add_resource(ObjDetResource, '/object_detection/')
api.add_resource(ObjDetVidResource, '/object_detection_vid/')
api.add_resource(SpeakerRecognitionResource, '/speaker_recognition/')
api.add_resource(VirusScannerResource, '/virus_scanner/')
api.add_resource(SceneClassificationResource, '/scene_classification/')
api.add_resource(SpeechToTextResource, '/speech_to_text/')
api.add_resource(OcrKerasResource, '/ocr_keras/')
api.add_resource(OcrTesseractResource, '/ocr_tesseract/')
api.add_resource(FullTextSearchResource, '/full_text_search/')
api.add_resource(OcrEasyResource, '/ocr_easy/')
api.add_resource(SentimentResource, '/text_sentiment/')
