from unittest import TestCase
import numpy as np
import pandas as pd
from apollo.models import Speaker
from apollo import MilvusHelper, PostgresDatabase
from speaker_recognition.vgg_speaker_recognition.main import SpeakerRecogAnalytic, SpeakerRecogRabbitConsumer


class TestMain(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.milvus_helper = MilvusHelper('speaker', vector_length=512)
        analytic = SpeakerRecogAnalytic('speaker_recognition', testing_in_jenkins=True)
        cls.rabbit_consumer = SpeakerRecogRabbitConsumer('speaker_recognition', 'ApolloExchange', analytic)

    @classmethod
    def tearDownClass(cls):
        # Wipe local speaker dbs
        cls.milvus_helper.milvus_instance.drop_collection('speaker')
        cls.milvus_helper = MilvusHelper('speaker', vector_length=512)
        cls.milvus_helper.close()
        cls.database = PostgresDatabase('apollo')
        cls.database.delete_all_from_table(Speaker)

    def test_SpeakerRecogRabbitConsumer_save_results_to_database(self):
        msg_dict = {'original_source': 's3://og_source.mp4',
                    'name': 's3://og_source_speaker1.mp3'}
        rando_array = np.random.rand(512)
        res_df = pd.DataFrame.from_dict({'person': ['Person1'], 'score': [0.99]})
        results_dict = {'dataframe': res_df,
                        'embedding_array': rando_array,
                        'mime_type': 'video'}
        self.rabbit_consumer.save_results_to_database(msg_dict, results_dict)

        # Check for the data in postgres and milvus
        pg_query_res = self.rabbit_consumer.database.query(Speaker, Speaker.original_source == msg_dict['original_source']).first()
        milvus_vector_id = pg_query_res.vector_id

        vector_exists = self.milvus_helper.check_vector_exists(milvus_vector_id)
        self.assertTrue(vector_exists)

    def test_SpeakerRecogRabbitConsumer_save_results_to_database_no_detected_speaker(self):
        # Test that save_results_to_database handles a case where there are no detected speakers
        msg_dict = {'original_source': 's3://no_audio.mp4',
                    'name': 's3://no_audio.mp4'}
        results_dict = {'dataframe': None, 'embedding_array': None}
        self.rabbit_consumer.save_results_to_database(msg_dict, results_dict)





