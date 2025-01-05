#!/usr/bin/env python
#
# This is a python2 program.
#
# Example run:
#   rabbit_worker.py -n <accessid> -k <accesskey> -o hello 
#
from __future__ import print_function

import sys
import ast
import boto3 as boto3
import argparse
import mpu.aws as mpuaws
import os
import shutil

from commandutils import rabbit_utils
from commandutils.ApolloMessage import ApolloMessage


S3_BUCKET_NAME = 'apollo-source-data'
S3_OUTPUT_DIR = 'outputs/speech_to_text'


def decode_audio (source, original_message):

    print("processing " + source, file=sys.stderr)
    sys.stderr.flush()

    #
    # Access S3 bucket
    #
    session = boto3.Session(
        aws_access_key_id=g_args.accessid,
        aws_secret_access_key=g_args.accesskey, 
        aws_session_token = None,
        region_name = "us-east-1", 
        botocore_session = None, 
        profile_name=None
    )
    s3 = session.client('s3')
    bucket_name, key = mpuaws._s3_path_split(source)

    #
    # Save wav file to ram disk
    #
    file_name = os.path.split (key)[1]
    local_inFile = "/dev/shm/" + str (os.getpid()) + "_" + file_name
    local_outFile = "/dev/shm/" + str (os.getpid()) + "_" + file_name + ".txt"
    try:
        s3.download_file (bucket_name, key, local_inFile)
        print ("downloaded %s to %s " % (key, local_inFile), file=sys.stderr)
        sys.stderr.flush()
    except:
        print ("Error could not download %s to %s " %
               (key, local_inFile), file=sys.stderr)
        sys.stderr.flush()
        return 

    #
    # Call decode_audio.bash for now.  TODO: don't have to restart server
    # each time.
    #
    cmd = "./decode_audio.bash %s > %s" % (local_inFile, local_outFile)
    print ("running '%s'" % cmd, file=sys.stderr)
    sys.stderr.flush()
    ret = os.system (cmd)
    assert (ret == 0)

    #
    # Save result to either a local file or back to S3
    #
    if g_args.out_file_name:
        # save to a local file
        shutil.copyfile (local_outFile, g_args.out_file_name)
        print("output saved to " + g_args.out_file_name, file=sys.stderr)
        sys.stderr.flush()
    else:
        # save to S3
        (path, extname) = os.path.splitext (key)
        (_, filename) = os.path.split(path)
        sys.stdout.flush()
        print(filename)
        s3_output_name = os.path.join(S3_OUTPUT_DIR, filename + extname.replace(".","_") + "_kaldi.txt")
        
        s3.upload_file (local_outFile, bucket_name, s3_output_name, ExtraArgs={'ContentType': "text/plain"})
        print("output saved to s3 bucket (%s) at %s" %
                (bucket_name, s3_output_name), file=sys.stderr)
        sys.stderr.flush()

        print("Sending message to named entity recognition")
        sys.stdout.flush()
        s3_file_path = "s3://" + S3_BUCKET_NAME + "/" + s3_output_name
        msg = {'name': s3_file_path, 'description': 'named_entity_recognition'}
        if 'original_source' in original_message:
            msg['original_source'] = original_message['original_source']
        else: 
            msg['original_source'] = msg['name']

        apollomsg = ApolloMessage(msg)
        apollomsg.publish('named_entity_recognition_route')
    
        
        

    #
    # clean up
    #
    os.remove (local_inFile)
    os.remove (local_outFile)


def callback(ch, method, properties, body):
    """
    We are looking for messages like below:
    '{"name": "s3://apollo-source-data/local/1272-128104-0000.wav", "description": "audio/x-wav"}'

    It can be read back as a python dictionary.
    """

    def sendAck ():
        if ch and method:
            ## send ack
            ch.basic_ack(delivery_tag=method.delivery_tag)
    
    ## debug: show the message:
    print(" [x] Received %r" % body, file=sys.stderr)
    sys.stderr.flush()

    ## Evalute the the message as a python statement:
    try:
        msg_dic = ast.literal_eval (body)
    except:
        print("message is not a valid Python expression", file=sys.stderr)
        sys.stderr.flush()
        sendAck()
        return
        
    if ('audio' in msg_dic['description']):
        decode_audio (msg_dic['name'], msg_dic)
    else:
        print("message is not an audio file", file=sys.stderr)
        sys.stderr.flush()

    ## debug: we are done
    sendAck()

    
def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-n', '--accessid', required=False,
                        help="access id, if not specified, " +
                        " get it from the environment variables or IAM")
    parser.add_argument('-k', '--accesskey', required=False,
                        help="access key if not specified, " +
                        " get it from the environment variables or IAM")
    parser.add_argument('-o', '--out_file_name', required=False,
                        help="Optional output text file. If not specified, " +
                        "will save back to S3 bucket. " +
                        "The s3 output name is determined from the input name.")
    parser.add_argument('-d', '--debug', help="run some debugging code",
                        action="store_true", default=False)
    args = parser.parse_args()

    return args


if __name__ == '__main__':
    #
    # Parse commandline options
    #
    g_args = parse_args()
        
    if g_args.debug:
        # body = '{"name": "s3://apollo-source-data/local/1272-128104-0000.wav", "description": "audio/x-wav"}'
        body = '{"name": "s3://apollo-source-data/local/bill_gates-TED.mp3", "description": "audio/mpeg"}'
        callback (None, None, None, body)
        sys.exit (os.EX_OK)

    #
    # connect to rabbitQ server and specify callback
    #

    rabbitmq_connection = rabbit_utils.get_rabbitmq_connection()

    channel = rabbitmq_connection.channel()
    sys.stderr.flush()

    channel.basic_qos(prefetch_count=1)
    channel.exchange_declare(exchange='ApolloExchange', durable=True)
    rabbit_utils.declare_queue_and_route(rabbitmq_connection, 'speech_to_text', 'ApolloExchange')
    rabbit_utils.declare_queue_and_route(rabbitmq_connection, 'named_entity_recognition', 'ApolloExchange')

    channel.basic_consume(queue='speech_to_text_queue', on_message_callback=callback)

    print("Waiting for rabbitMQ", file=sys.stderr)
    sys.stderr.flush()

    #
    # start getting rabbitQ messages
    #
    channel.start_consuming()
