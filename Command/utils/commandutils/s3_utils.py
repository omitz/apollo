import boto3
import sys
import os
import mpu.aws as mpuaws

S3_BUCKET = 'apollo-source-data'


def access_bucket_and_download(source, ram_storage):
    # Access S3 bucket
    session = boto3.Session(region_name="us-east-1")
    bucket, target = mpuaws._s3_path_split(source)
    # Download the required file from the s3 bucket
    s3 = session.client('s3')
    download(s3, bucket, target, ram_storage)
    return s3, target


def save_results_to_s3(local_outdir, s3, s3_output_dir):
    # Save results (in dev/shm/outdir/) to S3
    for file in os.listdir(local_outdir):
        local_outFile = os.path.join(local_outdir, file)
        s3_output_name = os.path.join(s3_output_dir, file)
        try:
            s3.upload_file(local_outFile, S3_BUCKET, s3_output_name)
            print("output saved to s3 bucket (%s) at %s" %
                  (S3_BUCKET, s3_output_name), file=sys.stderr)
            sys.stderr.flush()
        except:
            print("Error could not upload %s to %s " %
                  (local_outFile, s3_output_name), file=sys.stderr)
            sys.stderr.flush()


def download(s3, bucket, key, outdir):
    base = os.path.basename(key)
    local_inFile = os.path.join(outdir, base)
    s3.download_file(bucket, key, local_inFile)
    print(f'Downloaded {key} to {local_inFile}')
    sys.stdout.flush()


"""
    Read a file from an S3 source.

    Parameters
    ----------

    source : str
        Path starting with s3://, e.g. 's3://bucket-name/key/foo.bar'
    profile_name : str, optional
        AWS profile

    Returns
    -------
    content : bytes

    botocore.exceptions.NoCredentialsError
        Botocore is not able to find your credentials. Either specify
        profile_name or add the environment variables AWS_ACCESS_KEY_ID,
        AWS_SECRET_ACCESS_KEY and AWS_SESSION_TOKEN.
        See https://boto3.readthedocs.io/en/latest/guide/configuration.html
"""


def s3_read(source, profile_name=None):
    session = boto3.Session(profile_name=profile_name)
    s3 = session.client('s3')
    bucket_name, key = mpuaws._s3_path_split(source)
    s3_object = s3.get_object(Bucket=bucket_name, Key=key)
    body = s3_object['Body']
    return body.read()


"""
    Generator that iterates over all objects in a given s3 bucket

    See http://boto3.readthedocs.io/en/latest/reference/services/s3.html#S3.Client.list_objects_v2
    for return data format
    :param bucket: name of s3 bucket
    :return: dict of metadata for an object
"""


def iterate_bucket_items(bucket, directory=''):
    session = boto3.Session(region_name="us-east-1")
    client = session.client('s3')
    paginator = client.get_paginator('list_objects_v2')
    page_iterator = paginator.paginate(Bucket=bucket, Prefix=directory)

    for page in page_iterator:
        if page['KeyCount'] > 0:
            for item in page['Contents']:
                yield item
