"""
 
 docker build -t filecheck:latest .
 docker run --rm  --network apollo-net  -v ~/data:/data filecheck python file_checker_rb.py --input /data/test.png --accessid AKIAYZ..... --accesskey l7WGzdb+btGq3peY4yNTT....... --useS3 True

"""

import io
import time
import functools
import threading
import sys
import ast
import os

import boto3 as boto3
import magic
import argparse

import mpu.aws as mpuaws
from typing import BinaryIO

from apollo import RabbitConsumer
from apollo import Analytic
from apollo import ApolloMessage
from apollo import S3FileStore
from apollo import FileManager


S3_BUCKET_NAME = 'apollo-source-data'
S3_INPUT_DIRECTORY = 'inputs/file_type'

class FileChecker(object):

    def __init__(self):
        self.file_checker = magic.Magic(mime=True)

    def process_file(self, filepath: str) -> str:
        return self.file_checker.from_file(filepath)

    def process_buffer(self, filebytes: BinaryIO) -> str:
        return self.file_checker.from_buffer(filebytes)

class FileTypeAnalytic(Analytic):

    def __init__(self, name, filestore=None, filemanager=None):
        super().__init__(name)

        if filestore:
            self.filestore = filestore
        else:
            self.filestore = S3FileStore()

        if filemanager:
            self.filemanager = filemanager
        else:
            self.filemanager = FileManager()

    def run(self, s3_filename):
        """
        :param s3_filename: eg "s3://apollo-source-data/inputs/obj_det/test.jpg"
        """

        print("processing " + s3_filename, flush=True)
        bucket, prefix = mpuaws._s3_path_split(s3_filename)
        self.filestore.download_file(prefix, self.filemanager.ram_storage)
        _file = os.path.split(prefix)[1]

        results = process_file(os.path.join(self.filemanager.ram_storage, _file))
        return results

    def cleanup(self):
        self.filemanager.cleanup()

class FileTypeRabbitConsumer(RabbitConsumer):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def save_results_to_database(self, msg_dict: dict, results: dict):
        
        for queue in results['output_queues']:
            extra_args={'mime': results['mime_type']}

            if 'original_source' in msg_dict:
                extra_args['original_source'] = msg_dict['original_source']

            post_message_to_queue(queue, msg_dict['name'], None, extra_args)

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', help="File to process")
    parser.add_argument('-u', '--useS3', help="read files from s3")
    parser.add_argument('-r', '--rxOn', help="start message consumption", default='False')

    return parser.parse_args()

def post_message_to_queue(service_name, filename, description=None, extra_args={}):
    print(f"Sending message to {service_name}_queue", flush=True)
    sys.stdout.flush()

    if description:
        msg = {'name': filename, 'description': description}
    else:
        msg = {'name': filename, 'description': service_name}

    msg = {**msg, **extra_args}

    apollomsg = ApolloMessage(msg)
    apollomsg.publish(f'{service_name}_route')

def process_file(path):

    client = boto3.Session().client('s3')
    #boto3 will automatically use credentials from the environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY
    
    print("processing file: %r" % path)
    sys.stdout.flush()

    file_checker = FileChecker()
    with open(path, "rb") as input_file:
        mime_type = file_checker.process_buffer(filebytes=input_file.read())
        print("got mime type: %r" % mime_type)
        sys.stdout.flush()

    output_queues = []
    if mime_type.startswith("audio") or mime_type.startswith('video'):
        output_queues.append('speaker_recognition')
        output_queues.append('speech_to_text')

    if mime_type.startswith("image"):
        output_queues.append('facenet')
        output_queues.append('object_detection')
        output_queues.append('ocr_keras')
        output_queues.append('ocr_tesseract')
        output_queues.append('ocr_easy')
        output_queues.append('scene_places365')
        
    if mime_type.startswith("text") or \
            mime_type == 'application/pdf' or \
            mime_type == 'application/msword' or \
            mime_type == 'application/vnd.openxmlformats-officedocument.wordprocessingml.document': # for txt, pdf, doc, and docx files
        output_queues.append('named_entity_recognition')
        output_queues.append('full_text_search')

    if mime_type.startswith('text'):
        output_queues.append('text_sentiment')

    if mime_type.startswith('video'):
        output_queues.append('object_detection_vid')
        output_queues.append('face_vid')

    results = {'mime_type': mime_type, 'output_queues': output_queues}
    return results

def main():
    args = parse_args()
    file_checker = FileChecker() 

    # turn on the receiver to process incoming messages
    if args.rxOn:
        exchange_name = "ApolloExchange"
        analytic = FileTypeAnalytic("file_type")
        rabbit_consumer = FileTypeRabbitConsumer('file_type', 'ApolloExchange', analytic, heartbeat=180)
        rabbit_consumer.declare_queue_and_route('speaker_recognition', exchange_name)
        rabbit_consumer.declare_queue_and_route('facenet', exchange_name)
        rabbit_consumer.declare_queue_and_route('speech_to_text', exchange_name)
        rabbit_consumer.declare_queue_and_route('file_type', exchange_name)
        rabbit_consumer.declare_queue_and_route('object_detection', exchange_name)
        rabbit_consumer.declare_queue_and_route('ocr_tesseract', exchange_name)
        rabbit_consumer.declare_queue_and_route('ocr_keras', exchange_name)
        rabbit_consumer.declare_queue_and_route('ocr_easy', exchange_name)
        rabbit_consumer.declare_queue_and_route('named_entity_recognition', exchange_name)
        rabbit_consumer.declare_queue_and_route('landmark', exchange_name)
        rabbit_consumer.declare_queue_and_route('scene_places365', exchange_name)
        rabbit_consumer.declare_queue_and_route('full_text_search', exchange_name)
        rabbit_consumer.declare_queue_and_route('text_sentiment', exchange_name)

        rabbit_consumer.run()

if __name__ == '__main__':
    main()
