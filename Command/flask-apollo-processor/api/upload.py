import os
from io import BytesIO
import math

import boto3
from flask import Blueprint, Flask, current_app, jsonify, request
from flask_restful import Resource, Api, reqparse, abort
from flask_praetorian import auth_required
from werkzeug.datastructures import FileStorage
import requests
import json

from commandutils.FileStorageArgument import FileStorageArgument, upload_file
from commandutils import ApolloMessage
from .search import add_aggregated_results
from .search import RESULTS_PER_PAGE

upload_blueprint = Blueprint('upload', __name__)
api = Api(upload_blueprint)

ALLOWED_EXTENSIONS = ['jpg', 'jpeg', 'png']

FILE_CONTENT_TYPES = { # these will be used to set the content type of S3 object. It is binary by default.
    'jpg': 'image/jpeg',
    'jpeg': 'image/jpeg',
    'png': 'image/png',
    'bin': 'application/octet-stream',
    'csv': 'text/csv',
    'doc': 'application/msword',
    'docx': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'gz': 'application/gzip',
    'bz': 'application/x-bzip',
    'bz2': 'application/x-bzip2',
    'bmp': 'image/bmp',
    'gif': 'image/gif',
    'htm': 'text/html',
    'html': 'text/html',
    'jar': 'application/java-archive',
    'js': 'text/javascript',
    'json': 'application/json',
    'mp3': 'audio/mpeg',
    'mpeg': 'video/mpeg',
    'pdf': 'application/pdf',
    'php': 'application/x-httpd-php',
    'ppt': 'application/vnd.ms-powerpoint',
    'pptx': 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'rar': 'application/vnd.rar',
    'sh': 'application/x-sh',
    'svg': 'image/svg+xml',
    'tar': 'application/x-tar',
    'txt': 'text/plain',
    'wav': 'audio/wav',
    'weba': 'audio/webm',
    'webm': 'video/webm',
    'webp': 'image/webp',
    'xhtml': 'application/xhtml+xml',
    'xls': 'application/vnd.ms-excel',
    'xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'xml': 'application/xml',
    'zip': 'application/zip',
    '7z': 'application/x-7z-compressed'
}

def file_upload_parser():
    put_parser = reqparse.RequestParser()
    put_parser.add_argument('file', required=True, type=FileStorage, location='files')
    put_parser.add_argument('ignore_hash')
    return put_parser

def call_get(payload, url):
    response = requests.get(url, params=payload)
    print(f'response: {response}\nresponse type: {type(response)}')
    json_response = response.json()
    json_formatted_str = json.dumps(json_response, indent=2)
    return json_response

def upload_local_file_to_s3(_file):
    file_els = _file.filename.rsplit('.', 1)
    if len(file_els) > 1:
        extension = file_els[1].lower()
    else: # Handle case where filename has no extension
        extension = None

    # create a file object of the file
    bytes_file = BytesIO()
    _file.save(bytes_file)
    # key_name = '{0}.{1}'.format('some-name', extension)
    if extension and extension in FILE_CONTENT_TYPES:
        content_type = FILE_CONTENT_TYPES[extension]
    else:
        content_type = 'application/octet-stream'

    bucket_name = current_app.config['BUCKET_NAME']
    s3_url = upload_file(bytes_file, 'inputs/uploads/' + _file.filename, content_type, bucket_name)
    print(
        f'Upload...done uploading file={_file.filename} type={content_type} bucket={bucket_name} url={s3_url}',
        flush=True)
    return _file

def upload_local_image_to_s3(image):
    print('Enter Upload...PUT', flush=True)
    # TODO: a check on file size needs to be there.
    
    # check logo extension
    extension = image.filename.rsplit('.', 1)[1].lower()
    if '.' in image.filename and not extension in ALLOWED_EXTENSIONS:
        abort(400, message='File extension is not one of our supported types.')
    # create a file object of the image
    image_file = BytesIO()
    image.save(image_file)
    # key_name = '{0}.{1}'.format('some-name', extension)
    content_type = FILE_CONTENT_TYPES[extension]
    bucket_name = current_app.config['BUCKET_NAME']
    upload_to = 'inputs/uploads' # If this upload location gets changed, change relevant code in UI
    upload_key = os.path.join(upload_to, image.filename)
    logo_url = upload_file(image_file, upload_key, content_type, bucket_name)
    print(
        f'Upload...done uploading file={image.filename} type={content_type} bucket={bucket_name} url={logo_url}',
        flush=True)
    s3loc = os.path.join(bucket_name, upload_key)
    full_s3_path = 's3://' + s3loc
    return full_s3_path

class PaginatedUploadResource(Resource):

    def get_items_per_page(self, request):

        if 'items_per_page' in request.args and request.args['items_per_page']:
            return int(request.args['items_per_page'])
        else:
            return RESULTS_PER_PAGE
    
    def get_num_pages(self, request, results, key):

        items_per_page = self.get_items_per_page(request)

        return math.ceil(len(results[key]) / items_per_page)

    def paginate_results(self, request, results, key):

        start_index = 0
        end_index = len(results[key])

        if 'page' in request.args and request.args['page'] is not None:
            
            page = int(request.args['page'])

            items_per_page = self.get_items_per_page(request)

            start_index = page * items_per_page
            end_index = (page + 1) * items_per_page
            
            if start_index >= len(results[key]):
                start_index = len(results[key])
            if end_index > len(results[key]):
                end_index = len(results[key])

        paginated_results = { key: results[key][start_index:end_index] }

        return paginated_results

    @auth_required
    def put(self):

        args = self.put_parser.parse_args()
        if self.upload_file_type == 'image':
            full_s3_path = upload_local_image_to_s3(args['file'])
        else: 
            full_s3_path = 's3://' + current_app.config['BUCKET_NAME'] + '/inputs/uploads/' + upload_local_file_to_s3(args['file']).filename

        payload = {
                    'name': full_s3_path,
                    'description': self.description,
                    'add_to_db': 'False', 
                }
        print(f'Payload = {payload}', flush=True)

        #Search the face database for similar faces.
        get_response = call_get(payload, self.url)
        results = self.paginate_results(request, get_response, self.results_key)
        results['num_pages'] = self.get_num_pages(request, get_response, self.results_key)
        
        return jsonify(get_response)

class UploadFile(Resource):
    '''
    This method is used for uploading files into a storage.
    The storage at the moment is an s3 bucket but can be swapped out
    for other storage later on.
    '''
    #To test use this curl command: 
    #curl -X PUT  localhost:8080/upload/ -F 'image=@IMG_3195.png'
    put_parser = file_upload_parser()
    
    @auth_required
    def put(self):
        print('\nIn UploadFile', flush=True)
        print(f'Args:{request.args}', flush=True)
        headers_str = '\n'
        for header in request.headers:
            if not header[0].startswith('Authorization'):
                headers_str += f'{header}\n'
        print(f'Headers:{headers_str}', flush=True)
        print(f'Form:{request.form}', flush=True)
        print(f'Files:{request.files}', flush=True)
        print(f'Values:{request.values}', flush=True)
        args = self.put_parser.parse_args()
        print(f'UploadFile args: {args}', flush=True)
        ignore_hash = args['ignore_hash']
        print(f"ignore_hash: {ignore_hash}", flush=True)
        _file = upload_local_file_to_s3(args['file'])

        message_body = { 'name': 's3://' + current_app.config['BUCKET_NAME'] + '/inputs/uploads/' + _file.filename, 'description': 'virus_scanner' }
        if ignore_hash:
            message_body['ignore_hash'] = ignore_hash
        #Search the face database for similar faces.

        apollo_message = ApolloMessage.ApolloMessage(message_body)
        route = 'virus_scanner_route'

        log_message = f'Sending message to {route}, message: {apollo_message}'
        current_app.logger.info(log_message)

        current_app.rmq_publisher.publish(apollo_message, route)

        return json.dumps(apollo_message.__dict__)

class UploadFace(PaginatedUploadResource):
    '''
    This method is used for uploading files into a storage.
    The storage at the moment is an s3 bucket but can be swapped out
    for other storage later on.
    '''
    #To test use this curl command: 
    #curl -X PUT  localhost:8080/upload/image/ -F 'image=@IMG_3195.png'
    put_parser = file_upload_parser()
    upload_file_type = 'image'
    description = 'face'
    url = 'http://' + current_app.config['FACE_SEARCH_HOST'] + ':82/find'
    results_key = 'results'

    @auth_required
    def put(self):
        return super().put()


class UploadLandmark(PaginatedUploadResource):
    #To test use this curl command:
    #curl -X PUT  localhost:8080/upload/landmark/ -F 'image=@IMG_3195.png'
    # curl -X PUT https://api.apollo-cttso.com/upload/landmark/ -F 'image=@worcester_000055.jpg'
    put_parser = file_upload_parser()
    upload_file_type = 'image'
    description = 'landmark'
    url = "http://" + current_app.config['LANDMARK_SEARCH_HOST'] + ":83/search"
    results_key = 'landmarks'

    @auth_required
    def put(self):
        return super().put()


class UploadSpeaker(PaginatedUploadResource):
    
    #To test use this curl command:
    # curl -X PUT  localhost:8080/upload/speaker/ -H "authorization: Bearer yourauth" -F 'file=@ewan_mcgregor.wav'
    put_parser = file_upload_parser()
    description = 'speaker'
    upload_file_type = "audio/video"
    url = 'http://' + current_app.config['SPEAKER_SEARCH_HOST'] + ':85/find'
    results_key = 'results'

    @auth_required
    def put(self):
        return super().put()


class UploadedFilesResource(Resource):

    @auth_required
    def get(self):
        return list(self.get_files())

    def get_files(self):
        s3 = boto3.client('s3')
        while True:
            response = s3.list_objects_v2(Bucket=current_app.config['BUCKET_NAME'], Prefix='inputs/uploads/')
            for obj in response['Contents']:
                yield obj['Key']
            try:
                kwargs['ContinuationToken'] = response['NextContinuationToken']
            except KeyError:
                break

api.add_resource(UploadFace, '/image/')
api.add_resource(UploadLandmark, '/landmark/')
api.add_resource(UploadSpeaker, '/speaker/')
api.add_resource(UploadFile, '/')
api.add_resource(UploadedFilesResource, '/file_list/')