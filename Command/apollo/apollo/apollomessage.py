import sys
import json

from .rabbit_consumer import RabbitConsumer


class ApolloMessage(object):
    def __init__(self, msg):
        for k, v in msg.items():
            print('setting {}: {}'.format(k, v))
            sys.stdout.flush()
            setattr(self, k, v)

    def publish(self, routing_key):
        print(f"hasattr: {hasattr(self, 'original_source')}", flush=True)
        if not hasattr(self, 'original_source'):
            setattr(self, 'original_source', self.name)

        connection = RabbitConsumer.get_rabbitmq_connection()
        connection.channel().basic_publish(   
                                            exchange='ApolloExchange',
                                            routing_key=routing_key,
                                            body=json.dumps(self.__dict__)
                                        )