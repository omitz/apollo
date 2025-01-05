import pika
import json
import time
from flask import current_app

class Publisher:
    connection = None

    def init(self):

        exchange_name = current_app.config['RABBITMQ_EXCHANGE']
        rabbitmq_services = current_app.config['RABBITMQ_SUBSCRIBERS']

        if exchange_name:
            self.connection = _create_connection()
            channel = self.connection.channel()
            channel.exchange_declare(exchange=exchange_name, durable=True)
            sys.stderr.flush()
            channel.basic_qos(prefetch_count=1)

            for service_name in rabbitmq_services:
                rabbit_utils.declare_queue_and_route(rabbitmq_connection, service_name, exchange_name)

    def publish(self, apollomsg, routing_key):
        if not self.connection:
            self.connection = self._create_connection()
        try:
            channel = self.connection.channel()
        except (pika.exceptions.NoFreeChannels, pika.exceptions.ConnectionWrongStateError, pika.exceptions.StreamLostError):
            self.connection = self._create_connection()
            channel = self.connection.channel()

        channel.basic_publish(exchange='ApolloExchange',
                          routing_key=routing_key,
                          body=json.dumps(apollomsg.__dict__))
        
    def _create_connection(self):
        rabbitmq_host = current_app.config['RABBITMQ_HOST']
        rabbitmq_user = current_app.config['RABBITMQ_USER']
        rabbitmq_password = current_app.config['RABBITMQ_PASSWORD']

        print("Connecting to RabbitMQ at host " + rabbitmq_host)

        if rabbitmq_password and rabbitmq_user:
            credentials = pika.PlainCredentials(rabbitmq_user, rabbitmq_password)
            params = pika.ConnectionParameters(rabbitmq_host, '5672', '/', credentials)

        else:
            params = pika.ConnectionParameters(host=rabbitmq_host)

        try:
            return pika.BlockingConnection(params)

        except (pika.exceptions.IncompatibleProtocolError, pika.exceptions.AMQPConnectionError) as e:
            while True:
                time.sleep(5)
                print("attempting to connect to rabbitmq")
                try:
                    return pika.BlockingConnection(params, on_open_connection=self.open_channel)
                except (pika.exceptions.IncompatibleProtocolError, pika.exceptions.AMQPConnectionError):
                    continue