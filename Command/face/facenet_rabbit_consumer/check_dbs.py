# milvus version 0.10.1

'''
Assess inconsistencies between the postgres and milvus dbs.
To run this script locally:
    docker exec -ti command_facenet-rabbit-consumer_1 /bin/bash
    python face/facenet_rabbit_consumer/check_dbs.py
To run this script in the deployed container:
    WARNING There are code blocks in this script that delete all face data from pg and Milvus. Check that they are commented out.
    Run `kubectl get pods` to get the pod name for one of the facenet rabbit consumer pods.
    kubectl exec -ti <pod name> /bin/bash
    python face/facenet_rabbit_consumer/check_dbs.py
'''

from face.facenet.inference import get_facenet_collection_name
import numpy as np
from apollo import PostgresDatabase, MilvusHelper
from apollo.models import DetectedFace
from time import sleep

def main():
    # Connect to Milvus
    milvus_helper = MilvusHelper(get_facenet_collection_name(), None, False)

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
    pg_all = [vector_id[0] for vector_id in sess.query(DetectedFace.vector_id)]
    print(f'postgres all: {pg_all}', flush=True)
    print(f'num in postgres: {len(pg_all)}')
    pg.close()

    # List all in milvus but not postgres
    only_in_milvus = [id for id in all_vector_ids if id not in pg_all]

    # List all in postgres but not milvus
    only_in_pg = [id for id in pg_all if id not in all_vector_ids]

    print(f'in milvus but not postgres: {only_in_milvus}', flush=True)
    print(f'in postgres but not in milvus: {only_in_pg}', flush=True)

    # Delete all from Milvus
    # milvus_helper.milvus_instance.drop_collection(milvus_helper.collection_name)

    # Close the instance
    milvus_helper.close()

    # #
    # # Delete all faces from Postgres
    # from apollo import PostgresDatabase
    # from apollo.models import DetectedFace
    # # Connect to postgres
    # pg = PostgresDatabase('apollo')
    # pg.delete_all_from_table(DetectedFace)
    # pg.close()
    #
    # # Check all deleted in postgres
    # from apollo import PostgresDatabase
    # from apollo.models import DetectedFace
    # # connect to postgres
    # pg = PostgresDatabase('apollo')
    # # get list of all ids
    # sess = pg.get_session()
    # pg_all = [vector_id[0] for vector_id in sess.query(DetectedFace.vector_id)]
    # print(f'pg all: {pg_all}', flush=True)
    # pg.close()

if __name__ == '__main__':
    main()