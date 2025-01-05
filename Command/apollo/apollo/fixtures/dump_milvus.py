import os

from milvus import Milvus


def dump_milvus():
    """
    Delete all data in milvus
    """
    # Connect to Milvus
    milvus_host = os.getenv('MILVUS_HOST', 'milvus-release')
    milvus = Milvus(host=milvus_host, port='19530')
    facenet_collection = 'facenet'
    milvus.drop_collection(collection_name=facenet_collection)
    speaker_collection = 'speaker'
    milvus.drop_collection(collection_name=speaker_collection)

if __name__ == "__main__":
    dump_milvus()