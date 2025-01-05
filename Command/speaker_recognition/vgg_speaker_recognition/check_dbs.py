# milvus version 0.10.1

'''
Assess inconsistencies between the postgres and milvus dbs.
To run this script in the deployed container:
    WARNING There are code blocks in this script that delete all face data from pg and Milvus. Check that they are commented out.
    kubectl exec -ti <pod name> /bin/bash
    python speaker_recognition/vgg_speaker_recognition/check_dbs.py
'''

import numpy as np
from apollo import PostgresDatabase, MilvusHelper
from apollo.models import Speaker
from time import sleep

def main():
    # Connect to Milvus
    milvus_helper = MilvusHelper('speaker', None, False)

    # Get list of all Milvus ids
    stats1, stats2 = milvus_helper.milvus_instance.get_collection_stats(milvus_helper.collection_name)
    print(f'collection stats: {stats1}\n{stats2}', flush=True)
    sleep(2)
    num_vectors_in_db = stats2['partitions'][0]['row_count']
    print(f'num_vectors_in_db: {num_vectors_in_db}', flush=True)
    sleep(2)
    all_vector_ids = set()
    # We can't just ask for num_vectors_in_db results because Milvus only lets you request up to 2048 results. So we can loop over with a new random query vector until we have everything.
    while len(all_vector_ids) < num_vectors_in_db:
        emb_arr = np.random.rand(512).reshape(1, -1)
        milvus_all = milvus_helper.milvus_instance.search(collection_name=milvus_helper.collection_name, query_records=emb_arr, top_k=2048, params=milvus_helper.get_search_param())[1]
        for res in milvus_all[0]:
            all_vector_ids.add(res.id)
        print(f'num vector ids found: {len(all_vector_ids)}', flush=True)
        sleep(2)
    print(f'milvus all: {all_vector_ids}\n', flush=True)
    sleep(2)

    # Connect to postgres
    pg = PostgresDatabase('apollo')

    # Get list of all ids
    sess = pg.get_session()
    pg_all = [vector_id[0] for vector_id in sess.query(Speaker.vector_id)]
    pg_all_paths = [path for path in sess.query(Speaker.path)]
    print(f'all paths: {pg_all_paths}', flush=True)
    print(f'postgres all: {pg_all}', flush=True)
    print(f'num in postgres: {len(pg_all)}')
    pg.close()

    # List all in milvus but not postgres
    only_in_milvus = [id for id in all_vector_ids if id not in pg_all]

    # List all in postgres but not milvus
    only_in_pg = [id for id in pg_all if id not in all_vector_ids]

    print(f'in milvus but not postgres: {only_in_milvus}', flush=True)
    print(f'in postgres but not in milvus: {only_in_pg}', flush=True)

    # # Delete all from Milvus
    # milvus_helper.milvus_instance.drop_collection(milvus_helper.collection_name)

    # Close the instance
    milvus_helper.close()

    # # Delete all from Postgres
    # # Connect to postgres
    # pg = PostgresDatabase('apollo')
    # pg.delete_all_from_table(Speaker)
    # pg.close()

    # # Check all deleted in postgres
    # # Connect to postgres
    # pg = PostgresDatabase('apollo')
    # # Get list of all ids
    # sess = pg.get_session()
    # pg_all = [vector_id[0] for vector_id in sess.query(Speaker.vector_id)]
    # print(f'pg all: {pg_all}', flush=True)
    # pg.close()

if __name__ == '__main__':
    main()