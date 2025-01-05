from unittest import TestCase
import time
from keras import backend as K
from apollo import MilvusHelper, PostgresDatabase
from apollo.models import Speaker
from speaker_recognition.vgg_speaker_recognition.src.utils import predict_feature_vector
from speaker_recognition.vgg_speaker_recognition.analytic import SpeakerRecogAnalytic


class TestAnalytic(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.analytic = SpeakerRecogAnalytic('speaker_recognition', testing_in_jenkins=True)
        # Wipe local speaker dbs and recreate
        cls.milvus_helper = MilvusHelper('speaker', vector_length=512)
        cls.milvus_helper.milvus_instance.drop_collection('speaker')
        cls.milvus_helper.close()
        cls.milvus_helper = MilvusHelper('speaker', vector_length=512)
        cls.database = PostgresDatabase('apollo')
        cls.database.create_table_if_not_exists(Speaker.__table__)
        cls.database.delete_all_from_table(Speaker)

    @classmethod
    def tearDownClass(cls):
        # Wipe local speaker dbs
        cls.milvus_helper.milvus_instance.drop_collection('speaker')
        cls.milvus_helper = MilvusHelper('speaker', vector_length=512)
        cls.milvus_helper.close()
        cls.database = PostgresDatabase('apollo')
        cls.database.delete_all_from_table(Speaker)

    def test_get_closest_result_for_local_file(self):
        # Add some samples of various speakers
        samples = ['speaker_recognition/vgg_speaker_recognition/tests/test_files/i-made-49-million-dollars.mp3',
                   'speaker_recognition/vgg_speaker_recognition/vip/Ewan_McGregor/00002.wav',
                   'speaker_recognition/vgg_speaker_recognition/vip/Ewan_McGregor/00004.wav']
        for recording in samples:
            # Extract embedding
            embedding_array = predict_feature_vector(recording, [], self.analytic.get_model())[0]
            # Add to Milvus and postgres
            vector_ids = self.milvus_helper.insert_vectors(embedding_array)
            results_dict = {
                'path': recording,
                'prediction': '',
                'score': 1.0,
                'original_source': recording,
                'vector_id': vector_ids[0]
            }
            self.database.save_record_to_database(results_dict, Speaker)
            K.clear_session()
            time.sleep(2)
        # Query with a new sample
        query = 'speaker_recognition/vgg_speaker_recognition/vip/Ewan_McGregor/00117.wav'
        result = self.analytic.get_closest_results_for_local_file(query, self.analytic.get_model(), 1)[0]
        self.assertIn('Ewan_McGregor', result.path)

