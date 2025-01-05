import os, sys
import math
import random
from sklearn import preprocessing
from milvus import Milvus, MetricType, IndexType


class MilvusHelper:

    def __init__(self, collection_name, vector_length=None, init=True):
        self.collection_name = collection_name
        self.vector_length = vector_length
        self.milvus_instance = self.get_milvus_instance()
        self.estimated_num_vectors = 5000000000 # The nlist parameter is based on this value. Note: Per Milvus, "For 10 billion or larger datasets a Milvus cluster is needed."
        if init:
            self.init_collection()

    def init_collection(self):
        status_has_collection, ok = self.milvus_instance.has_collection(self.collection_name)
        if not ok:
            # Create the collection
            print('collection does not yet exist, creating', flush=True)
            param = {'collection_name': self.collection_name, 'dimension': self.vector_length, 'index_file_size': 1024,
                     'metric_type': MetricType.L2}
            self.milvus_instance.create_collection(param)
            nlist = self.get_nlist()
            ivf_param = {'nlist': nlist}
            self.milvus_instance.create_index(self.collection_name, IndexType.IVF_FLAT, ivf_param)

    def get_milvus_instance(self):
        milvus_host = os.getenv('MILVUS_HOST', 'milvus-release')
        return Milvus(host=milvus_host, port='19530')

    def get_nlist(self):
        nlist = round(4 * math.sqrt(self.estimated_num_vectors))
        return nlist

    def get_search_param(self):
        nprobe = 128
        nlist = self.get_nlist()
        if nprobe > nlist:
            nprobe = nlist
        search_param = {
            'nprobe': nprobe}  # Search <nprobe> vector classes (aka buckets, clusters) # Higher nprobe = higher precision but worse search efficiency, although that may differ per dataset with different distributions
        return search_param

    def insert_vectors(self, emb_array):
        '''
        Normalize and insert the vectors (one for each face or speaker) into Milvus
        :param emb_array: A 2d array
        :return: vector_ids: The vector ids assigned to the arrays by Milvus
        '''
        normalized = preprocessing.normalize(emb_array)
        # Milvus requires that all vector ids be assigned manually or all be assigned automatically (ie you cannot specify the id for some, and then let Milvus specify the id for others). Because restoring our database requires inserting vectors with manually assigned ids, we need to manually assign ids when doing the initial insert.
        stats1, stats2 = self.milvus_instance.get_collection_stats(self.collection_name)
        print(f'collection stats: {stats1}\n{stats2}', flush=True)

        assigned_ids = [self.generate_id() for _ in emb_array]
        if None in assigned_ids:
            # We've hit the maximum recursion depth. If we're hitting this point, one option is to increase the recursion limit. Note, hitting this point might indicate that we're getting close to `estimated_num_vectors`.
            raise Exception('Unable to generate assigned ids.')

        print(f'assigned ids: {assigned_ids}', flush=True)

        status, vector_ids = self.milvus_instance.insert(collection_name=self.collection_name, records=normalized, ids=assigned_ids)
        if not status.OK():
            msg = f'Vector not inserted. Status: {status}. Vector ids: {vector_ids}'
            raise Exception(msg)
        self.milvus_instance.flush([self.collection_name])
        return vector_ids

    def generate_id(self):
        '''
        :return: An integer representing an id in the Milvus database which has not been taken. Or 'None' if the function exceeds maximum recursion depth.
        Note: Milvus will allow a vector to be inserted with a taken id.
        '''
        success = False
        assigned_id = random.randint(1, sys.maxsize)
        print(f'Trying assigned id {assigned_id}', flush=True)
        # Check that these assigned id is not taken. (Unfortunately, the get_entity_by_id status.OK() will be true whether the id is in the collection or not.) If the id is not in the collection, the vector result of get_entity_by_id will be a list of empty lists.)
        status, vector = self.milvus_instance.get_entity_by_id(collection_name=self.collection_name, ids=[assigned_id])
        if not status.code == 1:  # If the collection is not empty
            vector_len = len(vector[0])
            if not vector_len == 0:
                print(f'Vector id {assigned_id} is taken.', flush=True)
                # Try again
                assigned_id = self.generate_id()
            else:
                success = True
        else:
            # The collection's empty, so any id is fine
            success = True
        if success:
            return assigned_id

    def check_vectors_inserted(self, vector_ids):
        for vector_id in vector_ids:
            exists = self.check_vector_exists(vector_id)
            if exists:
                print('Vector inserted successfully.', flush=True)
            else:
                raise Exception('Vector not inserted.')

    def check_vector_exists(self, vector_id):
        '''
        Based on the docs, it seems like you should be able to run get_entity_by_id and then check if the returned status is OK. However it seems there's a bug in pymilvus: get_entity_by_id will always return OK; if the vector isn't in the collection, it will return OK, and an empty byte string; if the vector is in the collection, it will return OK, and the vector.
        So, this is the workaround for that unexpected behavior.
        Returns: A boolean indicating whether or not the vector_id is in the collection.
        '''
        vector = self.get_vector_by_id(vector_id)
        if vector == b'':
            return False
        else:
            return True

    def get_vector_by_id(self, vector_id):
        '''
        :return: vector: An empty byte string (See check_vector_exists documentation) or a list containing a single vector
        '''
        status_vector_exists, vector = self.milvus_instance.get_entity_by_id(collection_name=self.collection_name,
                                                                             ids=[vector_id])
        return vector

    def query(self, num_milvus_results, emb_array):
        '''
        :param num_milvus_results: The number of result vectors to retrieve from the collection
        :param emb_array: A 2d array
        :return: top_k_result: A list containing tuples consisting of the id and vector distance (as in the distance between the query vector and the vector in the database). Eg   [[
                                                    (id:2, distance:1.0),
                                                    (id:5, distance:1.0)
                                                    ]]
        '''
        normalized = preprocessing.normalize(emb_array)
        # Query Milvus for similar faces and track result ids
        search_param = self.get_search_param()
        status, top_k_result = self.milvus_instance.search(collection_name=self.collection_name,
                                                      # See top_k_result.distance_array for the distance of each vector
                                                      query_records=normalized,
                                                      top_k=num_milvus_results,
                                                      params=search_param)
        return top_k_result

    def close(self):
        self.milvus_instance.close()