import os
import numpy as np
from milvus import Milvus
from time import sleep
import math
from apollo import S3FileStore


def create_milvus_fixture():
    """
    Create a file representation of facenet milvus collection and save to s3
    """
    # Delete any (local) previous milvus dump
    outfile = 'milvus_dump.txt'
    if os.path.exists(outfile):
        print('Removing (local) previous dump.')
        os.remove(outfile)

    # Connect to Milvus
    milvus_host = os.getenv('MILVUS_HOST', 'milvus-release')
    milvus_instance = Milvus(host=milvus_host, port='19530')

    facenet_collection = 'facenet'
    write_collection_to_file(milvus_instance, facenet_collection, outfile)
    speaker_collection = 'speaker'
    write_collection_to_file(milvus_instance, speaker_collection, outfile)

    filestore = S3FileStore()
    filestore.upload_file(outfile, 'data_fixtures/milvus_dump.txt')


def write_collection_to_file(milvus_instance, collection_name, outfile):
    # get list of all ids
    stats1, stats2 = milvus_instance.get_collection_stats(collection_name)
    try:
        num_vectors_in_db = stats2['partitions'][0]['row_count']
    except TypeError:
        num_vectors_in_db = 0
    print(f'Looking for {num_vectors_in_db} ids.')
    all_vector_ids = set()
    # We can't just ask for num_vectors_in_db results because Milvus only lets you request up to 2048 results. So we can loop over with a new random query vector until we have everything.
    while len(all_vector_ids) < num_vectors_in_db:
        emb_arr = np.random.uniform(low=-1, high=1, size=512).reshape(1, -1)
        milvus_all = milvus_instance.search(collection_name=collection_name, query_records=emb_arr, top_k=2048,
                                            params=get_search_param())[1]
        for res in milvus_all[0]:
            all_vector_ids.add(res.id)
        print(f'num vectors found in {collection_name}: {len(all_vector_ids)}', flush=True)
        sleep(2)
    print(f'total vectors found in {collection_name}: {len(all_vector_ids)}', flush=True)

    with open(outfile, 'a') as f:
        f.write(f'collection:{collection_name}\n')
        if len(all_vector_ids) > 0:
            vector_id_batches = list(gen_batches(list(all_vector_ids)))
            print(f'Processing {len(vector_id_batches)} batches for {collection_name}', flush=True)
            for ids_batch in vector_id_batches:
                status, vectors_batch = milvus_instance.get_entity_by_id(collection_name, ids_batch)
                print(f'status: {status}', flush=True)
                for i in range(len(vectors_batch)):
                    vector_id = ids_batch[i]
                    vector = vectors_batch[i]
                    f.write(f'{vector_id}:{vector}\n')


def gen_batches(vector_data):
    # Per Milvus, 'Input id array size cannot exceed 1000' for get_entity_by_id
    n = 1000
    for i in range(0, len(vector_data), n):
        yield vector_data[i:i + n]


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


if __name__ == '__main__':
    create_milvus_fixture()
