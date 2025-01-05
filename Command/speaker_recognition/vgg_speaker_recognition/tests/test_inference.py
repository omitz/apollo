from unittest import TestCase
from apollo import MilvusHelper, PostgresDatabase
from apollo.models import Speaker
from speaker_recognition.vgg_speaker_recognition.inference import load_files_dict, build_dict, inference


class TestInference(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.milvus_helper = MilvusHelper('speaker', vector_length=512)

    @classmethod
    def tearDownClass(cls):
        # Wipe local speaker dbs
        cls.milvus_helper.milvus_instance.drop_collection('speaker')
        cls.milvus_helper.close()
        cls.database = PostgresDatabase('apollo')
        cls.database.delete_all_from_table(Speaker)

    def test_inference(self):
        # Test that the analytic returns the correct speaker prediction
        results_dict = inference('speaker_recognition/vgg_speaker_recognition/tests/test_files/i-made-49-million-dollars.mp3')
        df = results_dict['dataframe']
        shape = results_dict['embedding_array'].shape
        print(f'emb array shape: \n{shape}', flush=True)
        prediction = df['person'].values[0]
        self.assertEqual(prediction, 'Leonardo_DiCaprio')

    def test_inference_no_audio(self):
        # Test that the analytic can run inference on a file with no audio without error
        results_dict = inference('speaker_recognition/vgg_speaker_recognition/tests/test_files/no_audio.mp4')
        df = results_dict['dataframe']
        self.assertIsNone(df)


