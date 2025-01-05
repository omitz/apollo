#!/usr/bin/env python3
#
# A more efficient version of database.py, which runs outside the
# docker container.  This version runs inside the container.
#
# 2020-06-24 (Wed)


#----------------------------------
# Preprocessor and Include Headers 
#----------------------------------
import os
import sys
import time
from commandutils import rabbit_utils, s3_utils



#---------------------------------------
# Global Static Variables and Constants
#---------------------------------------
S3_BUCKET_NAME = 'apollo-source-data'
S3_INPUT_DIRECTORY = 'inputs/load_test/iaprtc12/'


#-------------------------
# Private Implementations 
#-------------------------



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

    connection = rabbit_utils.get_rabbitmq_connection()
    channel = connection.channel()
    exchange_name = 'ApolloExchange'
    channel.exchange_declare(exchange=exchange_name, durable=True)
    rabbit_utils.declare_queue_and_route(connection, 'scene_places365', exchange_name)

    #
    # 1.) Parse the s3 directory to get all image files
    #
    for (idx, elm) in enumerate (s3_utils.iterate_bucket_items(bucket=S3_BUCKET_NAME,
                                                               directory=S3_INPUT_DIRECTORY)):
        key="path"
        val= elm["Key"]         # eg. inputs/load_test/iaprtc12/images/00/33.jpg
        if val.endswith(".jpg"):

            s3_path = f's3://{S3_BUCKET_NAME}/{val}'
            print(f"processing file: {s3_path}", flush=True)

            rabbit_utils.post_message_to_queue('scene_places365', s3_path)

            # only ingest a few for now
            if idx >= 200:
                break
        
    #---------------------------
    # program termination:
    #---------------------------

