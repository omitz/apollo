import os
from time import sleep
import math

import numpy as np
from milvus import Milvus, MetricType, IndexType

from apollo import S3FileStore


def get_search_param():
    nprobe = 128
    nlist = get_nlist()
    if nprobe > nlist:
        nprobe = nlist
    search_param = {
        'nprobe': nprobe}  # Search <nprobe> vector classes (aka buckets, clusters) # Higher nprobe = higher precision but worse search efficiency, although that may differ per dataset with different distributions
    return search_param

def get_nlist():
    estimated_num_vectors = 1000000
    nlist = round(4 * math.sqrt(estimated_num_vectors))
    return nlist

def load_milvus_fixture():
    """
    Download data file from s3 and load into milvus
    """
    
    filestore = S3FileStore()
    filestore.download_file('data_fixtures/milvus_dump.txt', './')

    # Connect to Milvus
    milvus_host = os.getenv('MILVUS_HOST', 'milvus-release')
    milvus = Milvus(host=milvus_host, port='19530')

    infile = 'milvus_dump.txt'

    with open(infile) as f:
        lines = f.readlines()
        collection_start_indices = [i for i in range(len(lines)) if lines[i].split(':')[0] == 'collection']
        print(f'collection start indices: {collection_start_indices}')
        collection_names = [lines[index].split(':')[1].replace('\n', '') for index in collection_start_indices]
        print(f'collection names: {collection_names}')
        for i, collection_name in enumerate(collection_names):
            vectors = []
            vector_ids = []
            if i == len(collection_names) - 1:  # if it's the last collection
                line_range = range(collection_start_indices[i] + 1, len(lines))
            else:
                line_range = range(collection_start_indices[i] + 1, collection_start_indices[i+1])
            for j in line_range:
                line = lines[j]
                split = line.split(':')
                vector_id = split[0]
                vector = list(np.fromstring(split[1][1:len(split[1]) - 2], sep=','))
                vector_ids.append(int(vector_id))
                vectors.append(vector)

            if len(vectors) > 0:
                status_has_collection, ok = milvus.has_collection(collection_name)
                if not ok:
                    vector_length = len(vectors[0])
                    param = {'collection_name': collection_name, 'dimension': vector_length, 'index_file_size': 1024, 'metric_type': MetricType.L2}
                    status = milvus.create_collection(param)
                    print(f'created collection status: {status}')

                    estimated_num_vectors = 1000000
                    nlist = round(4 * math.sqrt(estimated_num_vectors))
                    ivf_param = {'nlist': nlist}
                    milvus.create_index(collection_name, IndexType.IVF_FLAT, ivf_param)

                status, inserted_vector_ids = milvus.insert(collection_name=collection_name, records=vectors, ids=vector_ids)
                print(f'vectors inserted status: {status}')
                print(f'vectors_inserted: {len(inserted_vector_ids)}')
            else:
                print(f'No vectors to insert')

if __name__ == '__main__':
    load_milvus_fixture()
