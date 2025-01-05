import os
import mpu.aws as mpuaws
import magic
from apollo import Analytic, FileManager, MilvusHelper, PostgresDatabase, S3FileStore
from apollo.models import Speaker
from speaker_recognition.vgg_speaker_recognition.inference import inference, parse_args
from speaker_recognition.vgg_speaker_recognition.src.utils import get_default_params, predict_feature_vector
import speaker_recognition.vgg_speaker_recognition.src.model as model


class SpeakerRecogAnalytic(Analytic):
    def __init__(self, name, testing_in_jenkins=False):
        super().__init__(name)
        self.fm = FileManager()
        self.database = PostgresDatabase('apollo')
        if not testing_in_jenkins:
            self.s3filestore = S3FileStore()

    def run(self, msg_dict_name) -> dict:
        '''
        The rabbit consumer callback will call this `run` function, passing it one argument: msg_dict['name']
        '''
        local_path = self.download(msg_dict_name)

        results_dict = inference(local_path)

        # Do a mimetype check, which will get saved to postgres, so that it's easy to render only the relevant elements in the ui (speaker processes audio and video files)
        mimetype_checker = magic.Magic(mime=True)
        with open(local_path, 'rb') as input_file:
            mime_type = mimetype_checker.from_buffer(input_file.read())
        if mime_type.startswith('audio'):
            mime_type = 'audio'
        elif mime_type.startswith('video'):
            mime_type = 'video'
        else:
            msg = f'Unexpected mime_type: {mime_type}'
            raise TypeError(msg)
        results_dict['mime_type'] = mime_type

        self.cleanup(local_path)
        return results_dict

    def download(self, msg_dict_name):
        # Access S3 bucket
        bucket, target = mpuaws._s3_path_split(msg_dict_name)
        self.s3filestore.download_file(target, self.fm.ram_storage)
        base = os.path.basename(msg_dict_name)
        local_path = os.path.join(self.fm.ram_storage, base)
        return local_path

    def get_closest_results(self, s3_filename, num_results=10):
        # Extract the embedding
        local_path = self.download(s3_filename)
        network_eval = self.get_model()
        results = self.get_closest_results_for_local_file(local_path, network_eval, num_results)
        return results

    def get_closest_results_for_local_file(self, local_path, network_eval, num_results):
        embedding_array = predict_feature_vector(local_path, [], network_eval)[0]
        results = self.query_milvus(embedding_array, num_results)
        print(f'get_closest_result_for_local_file results: {results}', flush=True)
        return results

    def query_milvus(self, embedding_array, num_results):
        # Query Milvus for similar faces
        milvus_helper = MilvusHelper('speaker', None, False)
        milvus_results = milvus_helper.query(num_results, embedding_array)
        print(f'milvus_results: {milvus_results}', flush=True)
        results = self.get_postgres_records(milvus_results)
        milvus_helper.close()
        print(f'results: {results}', flush=True)
        return results

    def get_model(self):
        # Load up the Keras model
        params = get_default_params()
        network_eval = model.vggvox_resnet2d_icassp(input_dim=params['dim'],
                                                    num_class=params['n_classes'],
                                                    mode='eval')
        print('Loading model weights', flush=True)
        network_eval.load_weights(
            'speaker_recognition/vgg_speaker_recognition/model/resnet34_vlad8_ghost2_bdim512_deploy/weights.h5')
        return network_eval

    def get_postgres_records(self, milvus_results):
        results = []
        if len(milvus_results) > 0:
            milvus_results = milvus_results[0]
            milvus_ids = [result.id for result in milvus_results]
            # Submit one query (rather than querying in a for-loop). Doing this in a single query supports scalability. The order won't be maintained, but we can sort afterward
            sql_results = self.database.query(Speaker, Speaker.vector_id.in_(milvus_ids)).all()
            # Sort the postgres results according to the order of the milvus_ids
            results = []
            for milvus_id in milvus_ids:
                # We expect only one sql row per Milvus id, but to be safe, we'll handle cases where the same Milvus id is in a table multiple times.
                sql_query_result = [sql_result for sql_result in sql_results if sql_result.vector_id == milvus_id]
                results += sql_query_result
        return results

    def cleanup(self, local_path):
        if os.path.isfile(local_path):
            os.remove(local_path)