from obj_det_vid_rabbit_consumer import ObjDetVidRabbitConsumer
from obj_det_vid_analytic import ObjDetVidAnalytic


if __name__ == '__main__':

    analytic = ObjDetVidAnalytic('object_detection_vid')

    consumer = ObjDetVidRabbitConsumer('obj-det-vid-saver', 'ApolloExchange', analytic, heartbeat=0)

    consumer.run()