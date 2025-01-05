#!/usr/bin/env python3
#
# This program ingests images for testing the scene classification module.
#
# 2020-06-24 (Wed)


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import time
import boto3
import mpu.aws as mpuaws
import requests



#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
S3_BUCKET_NAME = 'apollo-source-data'
S3_INPUT_DIRECTORY = 'inputs/load_test/iaprtc12/'


#-------------------------
# Private Implementations 
#-------------------------
## Taken from Command/utils/commandutils/s3_utils.py
def iterate_bucket_items(bucket, directory=''):
    session = boto3.Session(region_name="us-east-1")
    client = session.client('s3')
    paginator = client.get_paginator('list_objects_v2')
    page_iterator = paginator.paginate(Bucket=bucket, Prefix=directory)

    for page in page_iterator:
        if page['KeyCount'] > 0:
            for item in page['Contents']:
                yield item



#-------------------------
# Public Implementations 
#-------------------------
if __name__ == "__main__":

    #------------------------------
    # parse command-line arguments:
    #------------------------------

    #---------------------------
    # run the program :
    #---------------------------
    #
    # 1.) Parse the s3 directory to get all image files
    #
    for (idx, elm) in enumerate (iterate_bucket_items(bucket=S3_BUCKET_NAME,
                                                      directory=S3_INPUT_DIRECTORY)):
        url="http://localhost:8080/jobs/scene_classification/"
        key="path"
        val= elm["Key"]         # eg. inputs/load_test/iaprtc12/images/00/33.jpg
        if val.endswith(".jpg"):
            print (f's3://{S3_BUCKET_NAME}/{elm["Key"]}')
            r = requests.post (url, json = {key:val})
            # time.sleep(1)
            print (r.text)

            # only ingest a few for now
            if idx >= 200:
                break
        # r.text      # response as a string
        # r.content   # response as a byte string
        #             #     gzip and deflate transfer-encodings automatically decoded 
        # r.json()    # return python object from json! this is what you probably want!
        
    #---------------------------
    # program termination:
    #---------------------------

