import argparse

import facenetrabbitconsumer
from face.facenet_analytic import facenetanalytic

def parse_args():
    parser = argparse.ArgumentParser()
    args = parser.parse_args()
    return args

def main(args):

    face_analytic = facenetanalytic.FacenetAnalytic('facenet')

    rabbit_consumer = facenetrabbitconsumer.FacenetRabbitConsumer('facenet', 'ApolloExchange', face_analytic, heartbeat=180)
    rabbit_consumer.run()

if __name__ == '__main__':

    args = parse_args()
    main(args)
