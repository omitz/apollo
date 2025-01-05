from unittest import TestCase
import numpy as np
from apollo import MilvusHelper


class TestMilvusHelper(TestCase):

    def test_query(self):
        collection_name = 'test'
        milvus_helper = MilvusHelper(collection_name, vector_length=5)
        # Insert some arrays
        arr1 = np.zeros(5)
        arr2 = np.ones(5)
        milvus_helper.insert_vectors([arr1, arr2])
        # Query an array which is closer to arr1 than arr2
        query_arr = np.array([0, 0, 0, 0, 1])
        top_k = milvus_helper.query(1, [query_arr])
        result = top_k[0][0]
        result_vector = milvus_helper.get_vector_by_id(result.id)[0]
        milvus_helper.milvus_instance.drop_collection(collection_name)
        milvus_helper.close()
        # Use np's almost_equal because Milvus performs quantization (ie changes the vector slightly)
        np.testing.assert_almost_equal(arr1, result_vector[0])