import sys
import os
import json
import pika
import time
from .ApolloMessage import ApolloMessage

"""
  send apollo msg
"""

def post_message_to_queue(service_name: str, filename : str,
                          description=None, extra_args={}):
    print(f"Sending message to {service_name}_queue")
    sys.stdout.flush()

    if description:
        msg = {'name': filename, 'description': description}
    else:
        msg = {'name': filename, 'description': service_name}
    
    msg = {**msg, **extra_args}

    apollomsg = ApolloMessage(msg)
    send_message(apollomsg, f'{service_name}_route')
    

def send_message(apollomsg, routing_key, connection=None):
    if not connection:
        connection = get_rabbitmq_connection()

    channel = connection.channel()
    channel.basic_publish(exchange='ApolloExchange',
                          routing_key=routing_key,
                          body=json.dumps(apollomsg.__dict__))


"""
    Creates a connection to the rabbitmq server

    Returns
    -------
    pika.connection.Connection

"""


def get_rabbitmq_connection(heartbeat=60, output=True):
    rabbitmq_host = os.getenv("RABBITMQ_HOST", "172.17.0.2")
    rabbitmq_user = os.getenv("RABBITMQ_USER")
    rabbitmq_password = os.getenv("RABBITMQ_PASSWORD")

    if output:
        print("Connecting to RabbitMQ at host " + rabbitmq_host + "...")
        sys.stdout.flush()

    if rabbitmq_password and rabbitmq_user:
        credentials = pika.PlainCredentials(rabbitmq_user, rabbitmq_password)
        params = pika.ConnectionParameters(rabbitmq_host, '5672', '/', credentials, heartbeat=heartbeat)

    else:
        params = pika.ConnectionParameters(host=rabbitmq_host, heartbeat=heartbeat)

    try:
        if output:
            print("Connected")
            sys.stdout.flush()

        return pika.BlockingConnection(params)

    except (pika.exceptions.IncompatibleProtocolError, pika.exceptions.AMQPConnectionError) as e:
        attempts = 10
        for i in range(attempts):
            time.sleep(5)
            print("attempt {}/{}: trying to connect to RabbitMQ again...".format(i+1, attempts))
            sys.stdout.flush()

            try:
                return pika.BlockingConnection(params)
            except (pika.exceptions.IncompatibleProtocolError, pika.exceptions.AMQPConnectionError) as e:
                print(e)
                sys.stdout.flush()
                continue
    
        return pika.BlockingConnection(params)


"""
    Declares a queue with a unique key and binds it to a route and exchange

"""


def declare_queue_and_route(connection, key, exchange_name, output=True):
    channel = connection.channel()
    if output:
        print("declaring queue: " + key + "_queue")
    queue = channel.queue_declare(key + '_queue', durable=True, arguments={'x-queue-type': 'classic'})
    channel.queue_bind(key + '_queue', exchange_name, routing_key=key + '_route')
    sys.stdout.flush()
    return queue

