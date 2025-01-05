#!/usr/bin/env python
#
# This is a python2 program.
#
from __future__ import print_function
import sys
import ast
import boto3 as boto3
import argparse
import mpu.aws as mpuaws
import os
import virus_scan

from apollo import Analytic
from apollo import ApolloMessage
from apollo import RabbitConsumer
from apollo import S3FileStore
from apollo import FileManager


S3_BUCKET = 'apollo-source-data'
S3_OUTPUT_DIR = 'outputs/virus_scan/'

def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-n', '--accessid', required=False,
                        help="access id, if not specified, " +
                             " get it from the environment variables or IAM")
    parser.add_argument('-k', '--accesskey', required=False,
                        help="access key if not specified, " +
                             " get it from the environment variables or IAM")
    parser.add_argument('-s3', '--useS3', required=False)
    args = parser.parse_args()
    return args

class VirusScannerAnalytic(Analytic):

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
        img_file = os.path.split(prefix)[1]
        input_file = os.path.join(self.filemanager.ram_storage, img_file)
        results = self.scan_file(input_file)

        self.filestore.upload_dir_content(self.filemanager.local_outdir, S3_OUTPUT_DIR)

        return results

    def get_closest_results(self, s3_filename, num_results=10):
        raise NotImplementedError

    def cleanup(self):
        self.filemanager.cleanup()

    def scan_file(self, filename):
        
        print(f"scanning '{filename}'" , flush=True)

        result = virus_scan.scan(filename)
        output_filename = virus_scan.get_output_filename(filename)
        virus_scan.write_to_file(output_filename, str(result))
        sys.stderr.flush()

        print("saving output to s3", flush=True)
        s3_output_name = os.path.join(S3_OUTPUT_DIR, output_filename)
        self.filestore.upload_file(output_filename, s3_output_name, extra_args={'ContentType': "text/plain"})

        return result

class VirusScannerRabbitConsumer(RabbitConsumer):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def save_results_to_database(self, msg_dict: dict, results: dict):
        
        s3_file_path = msg_dict['name']

        if results:
            print("File is safe, sending message to file_hash_queue")
            sys.stdout.flush()
            msg = {'name': s3_file_path, 'description': 'file_hash'}

            if 'ignore_hash' in msg_dict:
                msg['ignore_hash'] = msg_dict['ignore_hash']
            
            if 'original_source' in msg_dict:
                msg['original_source'] = msg_dict['original_source']

            message = ApolloMessage(msg)
            message.publish('file_hash_route')
        else:
            print("File is not safe")
            sys.stdout.flush()

def main(args):

    analytic = VirusScannerAnalytic('virus_scanner')
    rabbit_consumer = VirusScannerRabbitConsumer('virus_scanner', 'ApolloExchange', analytic, heartbeat=180)
    rabbit_consumer.run()

if __name__ == '__main__':

    args = parse_args()
    main(args)
