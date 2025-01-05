import sys
import os
import time
import socket
import json
import ast

import pika
import boto3
import argparse
import mpu.aws as mpuaws
from abc import ABC, abstractmethod

from .analytic import Analytic


class RabbitConsumer(ABC):
    """Base class to consume a rabbitmq queue and call an analytic"""


    def __init__(self, name: str, exchange_name: str, analytic: Analytic, heartbeat: int=60):
        self.name = name
        self.connection = self.get_rabbitmq_connection(heartbeat=heartbeat)
        print("Connected", flush=True)

        self.analytic = analytic
        self.exchange_name = exchange_name
        self.queue_name = self.analytic.name + '_queue'
        self.route_name = self.analytic.name + '_route'
        self.heartbeat = heartbeat
        self.channel = self.connection.channel()

        self.channel.exchange_declare(exchange=exchange_name, durable=True)

        self.channel.basic_qos(prefetch_count=1)
        self.declare_queue_and_route(self.analytic.name, exchange_name)
   
    def run(self):

        self.channel.basic_consume(self.queue_name, on_message_callback=self.callback)
        print(f"Waiting for rabbitMQ on queue {self.queue_name}", flush=True)

        # Start getting rabbitMQ messages
        self.channel.start_consuming()

    def declare_queue_and_route(self, key, exchange_name):
        """
        Declares a queue with a unique key and binds it to a route and exchange

        """
        
        queue_name = f"{key}_queue"
        route_name = f"{key}_route"
        print(f"declaring queue: {queue_name}")
        queue = self.channel.queue_declare(queue_name, durable=True, arguments={'x-queue-type': 'classic'})
        self.channel.queue_bind(queue_name, exchange_name, routing_key=route_name)
        return queue
 
    def callback(self, ch, method, properties, body):
        """
        Read a rabbitmq message, check if file exists in s3

        We expect messages like:
        {"name": "<s3 image path>", "num_milvus_results": 5, "description": "face"}

        It can be read back as a python dictionary.
        """

        # Show the message:
        print(f"[x] Received {body}")

        # Evaluate the the message as a python statement
        try:
            body = body.decode('utf-8')
            msg_dict = ast.literal_eval(body)
        except:
            print("message is not a valid Python expression", flush=True)
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print(f'sent acknowledgement to rabbitmq route {method.routing_key}')
            return

        if self.analytic.name.strip('-_') in msg_dict['description'].strip('-_'):
            # ignore dashes, underscores in description and service_name
            s3_client = boto3.client('s3')

            s3_file_path = msg_dict['name']
            print(f's3 filepath: {s3_file_path}', flush=True)
            bucket, prefix = mpuaws._s3_path_split(s3_file_path)

            try:
                s3 = boto3.resource('s3')
                print(f'about to load {bucket}, {prefix}')
                s3.Object(bucket, prefix).load()

            except s3_client.exceptions.ClientError as e:
                if e.response['Error']['Code'] == "404":
                    print(f"\"{s3_file_path}\" is not an s3 object that exists", flush=True)
            else:

                results = self.analytic.run(msg_dict['name'])
                self.save_results_to_database(msg_dict, results)
        else:
            print(f"input is not intended for {self.analytic.name}")

        # debug: we are done
        if ch and method:
            # send ack
            ch.basic_ack(delivery_tag=method.delivery_tag)
            print(f'sent acknowledgement to rabbitmq route {method.routing_key}', flush=True)

        
        print(f"[x] Finished {body}", flush=True)

    @abstractmethod
    def save_results_to_database(self, json_results: str):
        pass

    @staticmethod
    def name(self) -> str:
        """
        The service name

        Returns:
            name: str, the analytic name
        """
        return self.name

    @staticmethod
    def get_rabbitmq_connection(heartbeat=60):
        """
        Creates a connection to the rabbitmq server

        Returns
        -------
        pika.connection.Connection

        """
        rabbitmq_host = os.getenv("RABBITMQ_HOST", "172.17.0.2")
        rabbitmq_user = os.getenv("RABBITMQ_USER")
        rabbitmq_password = os.getenv("RABBITMQ_PASSWORD")

        print(f"Connecting to RabbitMQ at host {rabbitmq_host}...")

        #initial try to connect
        if rabbitmq_password and rabbitmq_user:
            credentials = pika.PlainCredentials(rabbitmq_user, rabbitmq_password)
            params = pika.ConnectionParameters(rabbitmq_host, '5672', '/', credentials, heartbeat=heartbeat)

        else:
            params = pika.ConnectionParameters(host=rabbitmq_host, heartbeat=heartbeat)

        try:
            return pika.BlockingConnection(params)

        except (pika.exceptions.IncompatibleProtocolError, pika.exceptions.AMQPConnectionError, socket.gaierror) as e:
            #try to connect to rabbitmq 10 times, sleeping 5 seconds in between
            attempts = 10
            for i in range(attempts):
                time.sleep(5)
                print(f"attempt {i+1}/{attempts}: trying to connect to RabbitMQ again...")

                try:
                    return pika.BlockingConnection(params)
                except (pika.exceptions.IncompatibleProtocolError, pika.exceptions.AMQPConnectionError, socket.gaierror):
                    continue
