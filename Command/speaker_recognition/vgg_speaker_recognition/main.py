import numpy as np
from apollo import Analytic, RabbitConsumer, S3FileStore, PostgresDatabase, FileManager, MilvusHelper
from apollo import RabbitConsumer, S3FileStore, PostgresDatabase, FileManager
from apollo.models.speaker import Speaker
from speaker_recognition.vgg_speaker_recognition.analytic import SpeakerRecogAnalytic


class SpeakerRecogRabbitConsumer(RabbitConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase(table=Speaker.__table__)

    def save_results_to_database(self, msg_dict, results_dict):
        '''
        The rabbit consumer callback will call this `save_results_to_database` function, passing it two arguments: msg_dict and the output of 'run'
        '''
        results_df = results_dict['dataframe']
        embedding_array = results_dict['embedding_array']
        if embedding_array is None:
            return

        embedding_array = np.asarray(results_dict['embedding_array'])

        # Save to Milvus
        # Initialize Milvus
        vector_length = len(embedding_array)
        milvus_helper = MilvusHelper('speaker', vector_length)

        # Insert the new vectors (embedding_array) into Milvus for future queries
        vector_ids = milvus_helper.insert_vectors([embedding_array])
        print(f'vector ids: {vector_ids}', flush=True)
        milvus_helper.check_vectors_inserted(vector_ids)

        # Save to postgres
        s3_path = msg_dict['name']
        prediction = results_df['person'].values[0]
        score = results_df['score'].values[0]
        if score < 0.855: # Note: This threshold was chosen arbitrarily and should be revisited if this model gets used long-term
            prediction = 'Unknown'
        if 'original_source' in msg_dict:
            source = msg_dict['original_source']
        else:
            source = msg_dict['name']
        results_dict = {
                            'path': s3_path,
                            'prediction': prediction,
                            'score': score,
                            'original_source': source,
                            'vector_id': vector_ids[0],
                            'mime_type': results_dict['mime_type']
                        }
        print(f"file: {results_dict['original_source']}")
        print(f"score: {results_dict['score']}")
        print(f"prediction: {results_dict['prediction']}", flush=True)
        self.database.save_record_to_database(results_dict, Speaker)
        milvus_helper.close()


if __name__ == '__main__':
    analytic = SpeakerRecogAnalytic('speaker_recognition')
    rabbit_consumer = SpeakerRecogRabbitConsumer('speaker_recognition', 'ApolloExchange', analytic)
    rabbit_consumer.run()