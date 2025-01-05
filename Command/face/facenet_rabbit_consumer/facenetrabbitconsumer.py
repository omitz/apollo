import numpy as np

from apollo import RabbitConsumer
from apollo import PostgresDatabase, MilvusHelper
from apollo.models import DetectedFace

from face.facenet.inference import get_facenet_collection_name


class FacenetRabbitConsumer(RabbitConsumer):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase('apollo', DetectedFace.__table__)

    def save_results_to_database(self, msg_dict: dict, results: dict):

        results_dataframe = results['dataframe']
        print(f'results df: \n{results_dataframe}', flush=True)

        embedding_array = np.asarray(results['embedding_array'])
        print(f'len emb array: {len(embedding_array)}', flush=True)
        if len(embedding_array) > 0:
            # Initialize Milvus
            vector_length = len(embedding_array[0])
            milvus_helper = MilvusHelper(get_facenet_collection_name(), vector_length)

            # Insert the new vectors (embedding_array) into Milvus for future queries
            vector_ids = milvus_helper.insert_vectors(embedding_array)
            print(f'vector ids: {vector_ids}', flush=True)
            milvus_helper.check_vectors_inserted(vector_ids)

        print(f'results df keys: {results_dataframe.keys()}', flush=True)
        print(f'len: {len(results_dataframe.keys())}', flush=True)

        for i in range(len(results_dataframe.keys())):
            print(f'i={i}:', flush=True)
            row = results_dataframe[i]
            print(f'row: {row}', flush=True)
            row['vector_id'] = vector_ids[i]

            if 'original_source' in msg_dict:
                row['original_source'] = msg_dict['original_source']
            else:
                row['original_source'] = row['path']

            self.database.save_record_to_database(row, DetectedFace)


