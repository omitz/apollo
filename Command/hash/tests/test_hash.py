import unittest
import os
import docker
import time
import sqlalchemy
from main import HashAnalytic, HashRabbitConsumer

from apollo.models import FileHash


class TestHash(unittest.TestCase):

    def create_rabbitmq_container(docker_client,
                                    api_client,
                                    timeout: int=30):
        rabbit_container = docker_client.containers.run(
            'rabbitmq:3.8.2-rc.1-management',
        
            ports={5672: 5672, 15672: 15672},
            healthcheck={
                "Test": ["CMD", "nc", "-z", "localhost", "5672"],
                "Interval": 1000000 * 1000
            },
            detach=True)

        health = None
        max_time = time.time() + timeout

        while health != "healthy" and (time.time() < max_time):
            inspection = api_client.inspect_container(rabbit_container.id)
            health = inspection['State']['Health']['Status']
            time.sleep(1)

        return rabbit_container

    def create_postgres_container(docker_client,
                                    api_client,
                                    password: str,
                                    timeout: int=30):
        '''Create a postgres docker container

        Uses health checks to ensure container is running
        and the postgres service is fully operational before
        returning.

        Timeout sets maximum number of seconds before giving
        up on a healthy container. Tests will obviously fail.
        '''

        postgres_container = docker_client.containers.run(
            'postgres:12.2',
            environment=[
                f"POSTGRES_PASSWORD={password}"
            ],
            ports={5432: 5432},
            healthcheck={
                "Test": ['CMD-SHELL', 'pg_isready -U postgres'],
                "Interval": 1000000 * 1000
            },
            detach=True)

        health = None
        max_time = time.time() + timeout

        while health != "healthy" and (time.time() < max_time):
            inspection = api_client.inspect_container(postgres_container.id)
            health = inspection['State']['Health']['Status']
            time.sleep(1)

        return postgres_container

    @classmethod
    def setUpClass(cls):
        os.environ['RABBITMQ_HOST'] = "rabbitmq"
        os.environ['POSTGRES_HOST'] = "postgres"
        os.environ['POSTGRES_PASSWORD'] = "secretpassword"

        cls.analytic = HashAnalytic('file_hash', testing_in_jenkins=True)
        cls.rabbit_consumer = HashRabbitConsumer('file_hash', 'ApolloExchange', cls.analytic)

    @classmethod
    def tearDownClass(cls):
        cls.rabbit_consumer.database.close()

    def setUp(self):
        connection = self.rabbit_consumer.connection
        channel = connection.channel()
        exchange_name = 'ApolloExchange'
        channel.exchange_declare(exchange=exchange_name, durable=True)
        self.rabbit_consumer.declare_queue_and_route('file_type', exchange_name)
        db_session = self.rabbit_consumer.database.get_session()
        db_session.query(FileHash).delete()
        db_session.commit()
        db_session.close()
        
    def tearDown(self):
        db_session = self.rabbit_consumer.database.get_session()
        db_session.query(FileHash).delete()
        db_session.commit()
        db_session.close()

        connection = self.rabbit_consumer.connection
        channel = connection.channel()
        channel.queue_delete(queue='file_type_queue')
        channel.queue_delete(queue='file_hash_queue')

    def test_get_hashes_image(self):
        result_hash_dict = self.analytic.get_hashes('tests/test_files/crowd.jpg')
        expected_hash = {'sha1': '8a877be0b4b15e7bc202f8afe8144134d303d7a2', 'sha256': '230ab795e2336e38c630b872b6239b6e6a25de39edd2a495e2fa944d26b0b045', 'sha512': '76a74f2a2eb569956a17d6389c7834e2ea8379147bd3f012a9ee2076bb5831b5e7079df0984896e1fc1efa72425d428ce875c172a40841e4c4e8935bfd9a45df', 'md5': '11f35c997643bf7134345b48d32bf673'}
        self.assertEqual(result_hash_dict, expected_hash)

    def test_get_hashes_audio_mp3(self):
        result_hash_dict = self.analytic.get_hashes('tests/test_files/bill_gates-TED.mp3')
        expected_hash = {'sha1': '08064b696660fdfb0d1409f131f1c3d77b0a287b', 'sha256': 'c9ae9a23f3e89f5bdc2e788e46822135c82f76f76cb38629d09f7f31fc6d1be2', 'sha512': '8d5dfbfc4efe5a9ca19b9fd9c519d9ab860bd5dfb3fca71545068a195f210b69a4da49ae1d6d99934975ddfb09789bf992621c6df9d3be462a6fc8a6d5ded363', 'md5': '502d3304883fa592af197af6a3d57493'}
        self.assertEqual(result_hash_dict, expected_hash)

    def test_get_hashes_audio_wav(self):
        result_hash_dict = self.analytic.get_hashes('tests/test_files/ewan_mcgregor.wav')
        expected_hash = {'sha1': '092aa58d09734f765cc154e5e795b6305d8c24c5', 'sha256': '2babcb5b3c84d2f7224853ec1da83317ace79ac9f38a3280bd9d11b5b28feee8', 'sha512': '3802e2be1781c404fb18993b407547a0b2bb53edf044326f0ce1ad143c2ce15ef5b2634bc36cd6235723e42d02c63eaea091a3d5e0cbc675d835a246653e6361', 'md5': '887a187c70e18f9a0f32ba66672816eb'}
        self.assertEqual(result_hash_dict, expected_hash)

    def test_get_hashes_text_txt(self):
        result_hash_dict = self.analytic.get_hashes('tests/test_files/test.txt')
        expected_hash = {'sha1': 'ffdf31fe715f9c47e70fd5e8de04ca4fc3cb5ec3', 'sha256': 'd409d2d2a8c3f615ba7e4c91c63da6d7d3654dfbc8819a47f45d9bd778711c61', 'sha512': '4ba7324d35f914e34319614fe787dbbf8784505e0b71fd224df6fc8d3ef932c027aed4ad0246394441fe6346ea3b8434ec18503058557eacfd83a895515897f0', 'md5': 'b275e9417a6c69482e719622175c6574'}
        self.assertEqual(result_hash_dict, expected_hash)

    def test_get_hashes_text_pdf(self):
        result_hash_dict = self.analytic.get_hashes('tests/test_files/test_pdf.pdf')
        expected_hash = {'sha1': 'e6890499e5b61bf14689381f5a8473cf7cfaa9cc', 'sha256': '588e82376cfd265562eed20441e3215a1da53c175ac06df5afd0189ac4f0bc14', 'sha512': 'bdc6fe0c1bd16a049ec4450da9676eaac3b249bb4e9745ea51f24e434608c11b94b993deb8ba69213f63a2e72a803e17b9e4ec6403be1658741a65e3d0f56d4c', 'md5': '0453c0e8c8746a56e5bcea13188c83bb'}
        self.assertEqual(result_hash_dict, expected_hash)

    def test_get_hashes_audio_text_doc(self):
        result_hash_dict = self.analytic.get_hashes('tests/test_files/test_doc.doc')
        expected_hash = {'sha1': '86a434619c5b65a7f127cc360c02dee81a8e6b14', 'sha256': '30abf062dd2f8853393eafadc883468822bd37cff92d2c4c0681ed7fbb26bf15', 'sha512': 'd6f494de311012098bd765690c8b0028d6b032e4ad91faa521d0537ea9c7c136030358c436b19ecbb55dfdffae3901aa63b105d4fe3b4506292036bdcafd2c9a', 'md5': 'cbb2e01bcd19163a1126cea9b184cf85'}
        self.assertEqual(result_hash_dict, expected_hash)
    
    def test_file_hash_already_exists(self):
        #save model to db
        result_hash_dict = self.analytic.get_hashes('tests/test_files/test_doc.doc')
        result_hash_dict['path'] = 's3://apollo-source-data/inputs/ner/test_doc.doc'
        self.rabbit_consumer.database.save_record_to_database(result_hash_dict, FileHash)

        file_hash_model = FileHash(**result_hash_dict)
        result = self.rabbit_consumer.file_hash_already_exists(file_hash_model)
        self.assertTrue(result)

    def test_file_hash_not_already_exists(self):
        #create model, dont save in db
        result_hash_dict = self.analytic.get_hashes('tests/test_files/test_doc.doc')
        result_hash_dict['path'] = 's3://apollo-source-data/inputs/ner/test_doc.doc'
        file_hash_model = FileHash(**result_hash_dict)

        result = self.rabbit_consumer.file_hash_already_exists(file_hash_model)
        self.assertFalse(result)

    def test_save_results_to_database_already_exists(self):
        s3path = 's3://apollo-source-data/inputs/ner/test_doc.doc'

        # save model to db
        result_hash_dict = self.analytic.get_hashes('tests/test_files/test_doc.doc')
        result_hash_dict['path'] = s3path
        self.rabbit_consumer.database.save_record_to_database(result_hash_dict, FileHash)

        # call save_results_to_database, which checks the file hash table for the hash, and if it hasn't been processed before, adds the file to the file_type queue
        msg_dict = {'name': s3path}
        self.rabbit_consumer.save_results_to_database(msg_dict, result_hash_dict)

        queue = self.rabbit_consumer.declare_queue_and_route('file_type', 'ApolloExchange')
        self.assertEqual(0, queue.method.message_count)

    def test_save_results_to_database_not_already_exists(self):
        s3path = 's3://apollo-source-data/inputs/ner/test_doc.doc'

        result_hash_dict = self.analytic.get_hashes('tests/test_files/test_doc.doc')

        # call save_results_to_database, which checks the file hash table for the hash, and if it hasn't been processed before, adds the file to the file_type queue
        msg_dict = {'name': s3path}
        self.rabbit_consumer.save_results_to_database(msg_dict, result_hash_dict)

        queue = self.rabbit_consumer.declare_queue_and_route('file_type', 'ApolloExchange')
        self.assertEqual(1, queue.method.message_count)