from __future__ import print_function
import sys
import ast
import boto3 as boto3
import argparse
import mpu.aws as mpuaws
import os, shutil
from commandutils import rabbit_utils, s3_utils


class RabbitWorker():
    exchange_name = ''
    service_name = ''
    service_fullname = ''

    queue_name = ''
    route_name = ''


    def __init__(self, exchange_name, service_name, heartbeat=60):

        rabbitmq_connection = rabbit_utils.get_rabbitmq_connection(heartbeat=heartbeat)
        
        self.exchange_name = exchange_name
        self.service_name = service_name.replace('-', '_')
        self.queue_name = self.service_name + '_queue'
        self.route_name = self.service_name + '_route'
        self.heartbeat = heartbeat

        self.channel = rabbitmq_connection.channel()
        self.channel.exchange_declare(exchange=exchange_name, durable=True)
        sys.stderr.flush()

        self.channel.basic_qos(prefetch_count=1)
        rabbit_utils.declare_queue_and_route(rabbitmq_connection, self.service_name, exchange_name)


    def get_callback(self, inference):
        def callback(ch, method, properties, body):
            """
            We are looking for messages like:
            {"name": "s3://apollo-source-data/inputs/ner/test.txt", "description": "ner"}

            It can be read back as a python dictionary.
            """

            # Show the message:
            print(" [x] Received %r" % body)
            sys.stdout.flush()
            sys.stderr.flush()

            # Evaluate the the message as a python statement
            try:
                body = body.decode('utf-8')
                msg_dic = ast.literal_eval(body)
            except:
                print("message is not a valid Python expression", file=sys.stderr)
                sys.stderr.flush()
                ch.basic_ack(delivery_tag=method.delivery_tag)
                print('sent acknowledgement to rabbitmq route {}'.format(method.routing_key))
                return

            output(f'msg dic: {msg_dic}')

            if self.service_name.strip('-').strip('_') in msg_dic['description'].strip('-').strip('_'):
                #ignore dashes, underscores in description and service_name
                s3_client = boto3.client('s3')	

                try:	
                    s3_file_path = msg_dic['name']	
                    bucket, prefix = mpuaws._s3_path_split(s3_file_path)	

                    try:	
                        s3 = boto3.resource('s3')	
                        s3.Object(bucket, prefix).load()      
                                        
                        inference(msg_dic['name'])
	
                    except s3_client.exceptions.ClientError as e:	
                        if e.response['Error']['Code'] == "404":	
                            print("\"{}\" is not an s3 object that exists".format(s3_file_path), file=sys.stderr)	
                            sys.stderr.flush()	

                except ValueError as e:	
                    print("\"{}\" is not a valid s3 object path".format(s3_file_path), file=sys.stderr)	
                    sys.stderr.flush()
            else:
                print("input is not intended for " + self.service_fullname + " detection", file=sys.stderr)
                sys.stderr.flush()

            # debug: we are done
            if ch and method:
                # send ack
                ch.basic_ack(delivery_tag=method.delivery_tag)
                print('sent acknowledgement to rabbitmq route {}'.format(method.routing_key))

            print(" [x] Finished %r" % body)
            sys.stdout.flush()
            sys.stderr.flush()

        return callback

    def work(self, inference):

        self.channel.basic_consume(queue=self.queue_name, on_message_callback=self.get_callback(inference))
        print("Waiting for rabbitMQ", file=sys.stderr)
        sys.stderr.flush()

        # Start getting rabbitMQ messages
        self.channel.start_consuming()


class RabbitWorkerArgs(RabbitWorker):
    '''
    Same as RabbitWorker except we pass the entire msg_dict to the inference function instead of just the file name.
    '''
    def get_callback(self, inference):
        def callback(ch, method, properties, body):
            """
            We expect messages like:
            {"name": "<s3 image path>", "num_milvus_results": 5, "description": "face"}

            It can be read back as a python dictionary.
            """

            # Show the message:
            print(" [x] Received %r" % body)
            sys.stdout.flush()
            sys.stderr.flush()

            # Evaluate the the message as a python statement
            try:
                body = body.decode('utf-8')
                msg_dic = ast.literal_eval(body)
            except:
                print("message is not a valid Python expression", file=sys.stderr)
                sys.stderr.flush()
                ch.basic_ack(delivery_tag=method.delivery_tag)
                print('sent acknowledgement to rabbitmq route {}'.format(method.routing_key))
                return

            if self.service_name.strip('-').strip('_') in msg_dic['description'].strip('-').strip('_'):
                # ignore dashes, underscores in description and service_name
                s3_client = boto3.client('s3')

                s3_file_path = msg_dic['name']
                print(f's3 filepath: {s3_file_path}')
                bucket, prefix = mpuaws._s3_path_split(s3_file_path)

                try:
                    s3 = boto3.resource('s3')
                    print(f'about to load {bucket}, {prefix}')
                    sys.stdout.flush()
                    s3.Object(bucket, prefix).load()

                    inference(msg_dic)

                except s3_client.exceptions.ClientError as e:
                    if e.response['Error']['Code'] == "404":
                        print("\"{}\" is not an s3 object that exists".format(s3_file_path), file=sys.stderr)
                        sys.stderr.flush()
            else:
                print("input is not intended for " + self.service_fullname + " detection", file=sys.stderr)
                sys.stderr.flush()

            # debug: we are done
            if ch and method:
                # send ack
                ch.basic_ack(delivery_tag=method.delivery_tag)
                print('sent acknowledgement to rabbitmq route {}'.format(method.routing_key))

            print(" [x] Finished %r" % body)
            sys.stdout.flush()
            sys.stderr.flush()
        return callback


def output(output):
    print(output)
    sys.stdout.flush()
   
