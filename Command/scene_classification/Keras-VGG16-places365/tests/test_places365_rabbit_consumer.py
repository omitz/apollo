from time import sleep
from unittest import TestCase
from unittest.mock import MagicMock
from places365_rabbit_consumer import Places365RabbitConsumer


class TestPlacesRabbitConsumer(TestCase):

    @classmethod
    def setUpClass(cls):
        # Initialize the analytic and rabbit consumer
        service_name = 'scene_places365'
        analytic = MagicMock()
        analytic.name = service_name
        cls.rabbit_consumer = Places365RabbitConsumer(service_name, 'ApolloExchange', analytic)
        cls.msg_dict = {}

        # Tear down the landmark queue (in case it exists before running this test)
        connection = cls.rabbit_consumer.connection
        channel = connection.channel()
        channel.queue_delete(queue='landmark_queue')

    @classmethod
    def tearDownClass(cls):
        cls.rabbit_consumer.database.close()

    @classmethod
    def setUp(self):
        # Connect to RabbitMQ
        connection = self.rabbit_consumer.connection
        channel = connection.channel()
        self.exchange_name = 'ApolloExchange'
        channel.exchange_declare(exchange=self.exchange_name, durable=True)
        landmark_queue = self.rabbit_consumer.declare_queue_and_route('landmark', self.exchange_name)
        sleep(2)

    @classmethod
    def tearDown(self):
        # Tear down the landmark queue
        connection = self.rabbit_consumer.connection
        channel = connection.channel()
        channel.queue_delete(queue='landmark_queue')

    def test_send_to_landmark(self):
        # Test that save_results_to_database sends an outdoor detection to the landmark queue.
        analytic_result = {'path': 's3://some_outdoor_scene.jpeg',
                           'class_hierarchy': 'outdoor',
                           'top_five_classes': [213, 49, 220, 260, 109]}
        self.rabbit_consumer.save_results_to_database({'name': 's3://some_outdoor_scene.jpeg'}, analytic_result)
        sleep(2)

        # Check that landmark queue has 1
        landmark_queue = self.rabbit_consumer.declare_queue_and_route('landmark', self.exchange_name)
        print(f'num msgs: {landmark_queue.method.message_count}', flush=True)
        self.assertEqual(1, landmark_queue.method.message_count)

    def test_dont_send_to_landmark(self):
        # Test that save_results_to_database does not send a non-outdoor detection to the landmark queue.
        analytic_result = {'path': 's3://some_indoor_scene.jpeg',
                           'class_hierarchy': 'indoor',
                           'top_five_classes': [213, 49, 220, 260, 109]}
        self.rabbit_consumer.save_results_to_database({'name': 's3://some_indoor_scene.jpeg'}, analytic_result)
        sleep(2)

        # Check that landmark queue has 0
        landmark_queue = self.rabbit_consumer.declare_queue_and_route('landmark', self.exchange_name)
        self.assertEqual(0, landmark_queue.method.message_count)