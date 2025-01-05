import os
import boto3
import mpu.aws as mpuaws
from apollo import Analytic, RabbitConsumer, S3FileStore, PostgresDatabase, ApolloMessage, FileManager
from apollo.models import FileHash
import hashlib


class HashAnalytic(Analytic):
    def __init__(self, name, testing_in_jenkins=False):
        super().__init__(name)
        self.fm = FileManager()
        if not testing_in_jenkins:
            self.s3filestore = S3FileStore()

    def run(self, msg_dict_name) -> dict:
        '''
        The rabbit consumer callback will call this `run` function, passing it one argument: msg_dict['name']
        '''
        # Access S3 bucket
        bucket, target = mpuaws._s3_path_split(msg_dict_name)
        self.s3filestore.download_file(target, self.fm.ram_storage)

        base = os.path.basename(msg_dict_name)
        local_path = os.path.join(self.fm.ram_storage, base)

        file_hashes = self.get_hashes(local_path)
        self.cleanup(local_path)
        return file_hashes

    def get_hashes(self, filename) -> dict:
        '''Calculate the hashes for the file'''

        sha1 = hashlib.sha1()
        sha256 = hashlib.sha256()
        sha512 = hashlib.sha512()
        md5 = hashlib.md5()

        BUF_SIZE = 65536  # 64 * 1024
        with open(filename, 'rb') as f:
            while True:
                data = f.read(BUF_SIZE)
                if not data:
                    break

                sha1.update(data)
                sha256.update(data)
                sha512.update(data)
                md5.update(data)

            sha1_hash = sha1.hexdigest()
            sha256_hash = sha256.hexdigest()
            sha512_hash = sha512.hexdigest()
            md5_hash = md5.hexdigest()

            return {'sha1': sha1_hash,
                    'sha256': sha256_hash,
                    'sha512': sha512_hash,
                    'md5': md5_hash}

    def cleanup(self, local_path):
        if os.path.isfile(local_path):
            os.remove(local_path)


class HashRabbitConsumer(RabbitConsumer):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase(table=FileHash.__table__)

    def save_results_to_database(self, msg_dict, file_hashes):
        '''
        The rabbit consumer callback will call this `save_results_to_database` function, passing it two arguments: msg_dict and the output of 'run'
        '''
        s3_path = msg_dict['name']
        file_hashes['path'] = s3_path
        file_hash_model = FileHash(**file_hashes)
        exists = self.file_hash_already_exists(file_hash_model)

        ignore_hash_val = msg_dict.get('ignore_hash', 'false')

        if (ignore_hash_val == 'true') or not exists:
            
            print(f'Sending message to file_type_queue', flush=True)
            new_msg = {'name': s3_path, 'description': 'file_type'}
            if 'original_source' in msg_dict:
                new_msg['original_source'] = msg_dict['original_source']
                
            apollomsg = ApolloMessage(new_msg)
            apollomsg.publish('file_type_route')

        if exists:
            print(f'File hash already exists for {s3_path}.', flush=True)
        else:
            self.database.save_record_to_database(file_hashes, FileHash)

    def file_hash_already_exists(self, file_hash_model: FileHash, echo: bool = False) -> bool:
        db_session = self.database.get_session()
        result = db_session.query(
            db_session.query(FileHash).filter(FileHash.sha1 == file_hash_model.sha1).exists()).scalar()
        db_session.close()
        return result


if __name__ == '__main__':
    analytic = HashAnalytic('file_hash')
    rabbit_consumer = HashRabbitConsumer('file_hash', 'ApolloExchange', analytic)
    rabbit_consumer.run()