#!/usr/bin/env python3
#
import sys
import ast
import boto3 as boto3
import argparse
import mpu.aws as mpuaws
import os
import ocr_keras_wrapper

from commandutils import rabbit_utils, s3_utils
from commandutils.ApolloMessage import ApolloMessage
from commandutils.rabbit_worker import RabbitWorker

S3_BUCKET = 'apollo-source-data'
S3_OUTPUT_DIR = 'outputs/ocr/'


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-n', '--accessid', required=False,
                        help="access id, if not specified, " +
                             " get it from the environment variables or IAM")
    parser.add_argument('-k', '--accesskey', required=False,
                        help="access key if not specified, " +
                             " get it from the environment variables or IAM")
    parser.add_argument('-s3', '--useS3', required=False)
    parser.add_argument('-d', '--debug', required=False, action="store_true")
    args = parser.parse_args()
    return args

def ocr_file(s3_file_path):
    """
    :param s3_file_path: eg "s3://apollo-source-data/local/face/imgs" or 
    "s3://apollo-source-data/local/face/imgs/Ewan_Mcgregor.jpg" 
    """
    
    #
    # Access S3 bucket and save file to ram disk:
    #
    bucket, key = mpuaws._s3_path_split(s3_file_path)
    s3 = boto3.resource('s3')
    s3.Object(bucket, key).load()
    s3_client = boto3.client('s3')
    ram_storage = '/dev/shm'
    s3_utils.download(s3_client, bucket, key, ram_storage)
    filename = os.path.split(key)[1]
    local_inPath = os.path.join (ram_storage, filename)
    print("saved to '%s' " % local_inPath, flush=True)

    #
    # OCR the image file:
    #
    (basepath, extname) = os.path.splitext (key)
    (_, basename) = os.path.split(basepath)
    local_outFile = basename + extname.replace(".","_") + "_ocr-keras.txt"
    local_outPath = os.path.join (ram_storage, local_outFile)

    local_outImgFile = basename + extname.replace(".","_") + "_ocr-keras-overlay.png"
    local_outImgPath = os.path.join (ram_storage, local_outImgFile)
    ocr_keras_wrapper.RunOcr (local_inPath, local_outPath, local_outImgPath)
    

    #
    # Save result (both text and image overlay) back to S3
    #
    s3_output_path = os.path.join (S3_OUTPUT_DIR, local_outFile)
    s3_client.upload_file (local_outPath, S3_BUCKET,
                           s3_output_path, ExtraArgs={'ContentType': "text/plain"})
    print ("uploading Txt to '%s' " % s3_output_path, flush=True)

    s3_output_path = os.path.join (S3_OUTPUT_DIR, local_outImgFile)
    s3_client.upload_file (local_outImgPath, S3_BUCKET,
                           s3_output_path, ExtraArgs={'ContentType': "image/png"})
    print ("uploading Img to '%s' " % s3_output_path, flush=True)
    
    
    #
    # clean up
    #
    print("cleaning up", flush=True)
    os.remove (local_inPath)
    os.remove (local_outPath)
    os.remove (local_outImgPath)


if __name__ == '__main__':

    args = parse_args()

    if args.debug:
        ocr_keras_wrapper.RunOcr ("Army_Reserves_Recruitment_Banner_MOD_45156284.jpg", "out.txt")
        sys.exit (0)
    
    bunny = RabbitWorker('ApolloExchange', 'ocr_keras')
    bunny.service_fullname = 'Keras OCR'
    bunny.work(ocr_file)
