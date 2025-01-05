import os
import mpu.aws as mpuaws
from apollo import Analytic, RabbitConsumer, S3FileStore, FileManager, PostgresDatabase
from apollo.models import Landmark
import extract_features


class LandmarkAnalytic(Analytic):

    def __init__(self, name, testing_in_jenkins=False):
        super().__init__(name)
        self.fm = FileManager()
        if not testing_in_jenkins:
            print(f'Initializing S3 FileStore', flush=True)
            self.s3filestore = S3FileStore()

    def run(self, full_s3_filepath):
        bucket, target = mpuaws._s3_path_split(full_s3_filepath)
        # Download the required file from the s3 bucket
        self.s3filestore.download_file(target, self.fm.ram_storage)
        img_filename = os.path.basename(target)
        local_img_path = os.path.join(self.fm.ram_storage, img_filename)
        # Extract features for the image
        img_dicts, features_found = extract_features.extract_feats([local_img_path],
                                                                source_paths=[full_s3_filepath])
        if not features_found:
            print(f'No features were found in {full_s3_filepath}', flush=True)
        self.cleanup(local_img_path)
        return (img_dicts, features_found)

    def cleanup(self, local_img_path):
        os.remove(local_img_path)


class LandmarkRabbitConsumer(RabbitConsumer):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase('apollo', Landmark.__table__)

    def save_results_to_database(self, msg_dict, result_from_analytic_run):
        img_dicts, features_found = result_from_analytic_run
        if features_found:
            for img_dict in img_dicts:
                if 'original_source' in msg_dict:
                    img_dict['original_source'] = msg_dict['original_source']
                else:
                    img_dict['original_source'] = msg_dict['name']
                self.database.save_record_to_database(img_dict, Landmark)

def main():

    analytic = LandmarkAnalytic('landmark')
    rabbit_consumer = LandmarkRabbitConsumer('landmark', 'ApolloExchange', analytic, heartbeat=60*10)
    rabbit_consumer.run()

if __name__ == '__main__':

    main()
