from flask_restful import  reqparse
from werkzeug.datastructures import FileStorage

from boto.s3.connection import S3Connection
from boto.s3.key import Key as S3Key

import os, sys

class FileStorageArgument(reqparse.Argument):
    """This argument class for flask-restful will be used in
    all cases where file uploads need to be handled."""
    
    def convert(self, value, op):
        print(f'value: {value}', flush=True)
        if self.type is FileStorage:  # only in the case of files
            # this is done as self.type(value) makes the name attribute of the
            # FileStorage object same as argument name and value is a FileStorage
            # object itself anyways
            return value

        # called so that this argument class will also be useful in
        # cases when argument type is not a file.
        super(FileStorageArgument, self).convert(*args, **kwargs)
        





def upload_file(fileContent, key_name,  content_type, bucket_name):

    logo_url = upload_s3(fileContent, key_name, content_type, bucket_name)

    return {'logo_url': logo_url}


# Try to convert to boto3
""" def upload_s3_ex(fileContent, key_name, content_type, bucket_name):
        # Access S3 bucket
    session = boto3.Session(
        aws_access_key_id=current_app.config['AWS_ACCESS_KEY_ID'],
        aws_secret_access_key=current_app.config['AWS_SECRET_ACCESS_KEY'],
        aws_session_token=None,
        region_name="us-east-1",
        botocore_session=None,
        profile_name=None
    )

    s3 = boto3.client('s3')
    s3.upload_fileobj(  fileContent, 
                        bucket_name, 
                        key_name,
                        ExtraArgs={"Content_type": content_type,"ACL": 'public-read'} 
                        )

    fileContent.close()


    return  """





## Helper Methods
def upload_s3(file, key_name, content_type, bucket_name):
    """Uploads a given StringIO object to S3. Closes the file after upload.
    Returns the URL for the object uploaded.
    Note: The acl for the file is set as 'public-acl' for the file uploaded.
    Keyword Arguments:
    file -- StringIO object which needs to be uploaded.
    key_name -- key name to be kept in S3.
    content_type -- content type that needs to be set for the S3 object.
    bucket_name -- name of the bucket where file needs to be uploaded.
    """
    print("Enter upload_s3.....")


    # create connection
    conn = S3Connection(os.getenv('AWS_ACCESS_KEY_ID'), os.getenv('AWS_SECRET_ACCESS_KEY'))

    # upload the file after getting the right bucket
    bucket = conn.get_bucket(bucket_name)
    obj = S3Key(bucket)
    obj.name = key_name
    obj.content_type = content_type
    obj.set_contents_from_string(file.getvalue())

    # close stringio object
    file.close()

    return obj.generate_url(expires_in=0, query_auth=False)