import argparse

from obj_det.object_detection_rabbit_consumer.objectdetectionrabbitconsumer import ObjectDetectionRabbitConsumer
from obj_det.object_detection_analytic.objectdetectionanalytic import ObjectDetectionAnalytic

def parse_args():
    parser = argparse.ArgumentParser()
    args = parser.parse_args()
    return args

def main(args):

    analytic = ObjectDetectionAnalytic('object_detection')

    rabbit_consumer = ObjectDetectionRabbitConsumer('object_detection', 'ApolloExchange', analytic, heartbeat=180)
    rabbit_consumer.run()

if __name__ == '__main__':

    args = parse_args()
    main(args)
