import boto3
import botocore
import os
import time

from .filestore import FileStore


class S3FileStore(FileStore):
    """ S3 file storage """

    def __init__(self, url=None, bucket_name=None):
        super().__init__()

        self.s3_session = boto3.Session(
            region_name="us-east-1",
        )
        
        self.s3_client = self.s3_session.client('s3', endpoint_url=url)
        self.s3_resource = boto3.resource('s3', endpoint_url=url)
        self.wait_until_available()

        if bucket_name:
            self.s3_bucket = bucket_name
        else:
            self.s3_bucket = 'apollo-source-data' #TODO: get from config file or env variable or something

        self.s3_client.create_bucket(Bucket=self.s3_bucket)

    def wait_until_available(self):
        """
        wait for filestore to be available
        """
        
        ready = False
        retries = 10
        while not ready and retries > 0:
            try:
                result = self.s3_client.list_buckets()
                ready = True
            except (botocore.exceptions.EndpointConnectionError, botocore.exceptions.ClientError) as ex:
                time.sleep(1)
                ready = False
                retries = retries - 1
                print("trying to connect to s3 again...")

    def download_file(self, file_path, outdir):
        '''
        Args:
            file_path: S3 filepath starting from after the bucket, eg "inputs/ner/test.txt"
        '''
        base = os.path.basename(file_path)
        local_inFile = os.path.join(outdir, base)
        self.s3_client.download_file(self.s3_bucket, file_path, local_inFile)
        print(f'Downloaded {file_path} to {local_inFile}', flush=True)

    def upload_file(self, local_outfile_path, upload_path, extra_args={}):
        self.s3_client.upload_file(local_outfile_path, self.s3_bucket, upload_path, ExtraArgs=extra_args)

    def upload_dir_content(self, local_outdir, s3_output_dir):
        """
        Args:
          local_outdir: eg. "/dev/shm/outdir/"
          s3_output_dir: eg. "outputs/scene_classification/"
        """
        for file in os.listdir (local_outdir):
            local_outFile = os.path.join(local_outdir, file)
            s3_output_name = os.path.join(s3_output_dir, file)
            try:
                self.upload_file (local_outFile, s3_output_name)
            except:
                print("Error could not upload %s to %s " %
                      (local_outFile, s3_output_name), flush=True)
        
    def download_dir_r(self, s3_path, s3_dir, local):

        paginator = self.s3_client.get_paginator('list_objects')
        for result in paginator.paginate(Bucket=self.s3_bucket, Delimiter='/', Prefix=s3_dir):
            if result.get('CommonPrefixes') is not None:
                for subdir in result.get('CommonPrefixes'):
                    self.download_dir_r(s3_path, subdir.get('Prefix'), local)
            for file in result.get('Contents', []):
                common_prefix = os.path.commonprefix([s3_path, file.get('Key')])
                relative_path = os.path.relpath(file.get('Key'), common_prefix)
                dest_pathname = os.path.join(local, relative_path)
                if not os.path.exists(os.path.dirname(dest_pathname)):
                    os.makedirs(os.path.dirname(dest_pathname))
                print(f"downloading {file.get('Key')} to {dest_pathname}", flush=True)
                self.s3_resource.meta.client.download_file(self.s3_bucket, file.get('Key'), dest_pathname)

    def download_dir(self, dist, local):
        self.download_dir_r(dist, dist, local)

    def delete_file(self, file_path):
        self.s3_client.delete_object(Bucket=self.s3_bucket, Key=file_path)

    def key_exists(self, key):
        try:
            self.s3_client.head_object(Bucket=self.s3_bucket, Key=key)
            return True
        except botocore.errorfactory.ClientError as error:
            try:
                if error.response['ResponseMetadata']['HTTPStatusCode'] == 404:
                    return False
            except Exception as error2:
                raise error
