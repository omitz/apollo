'''
Run every file in S3 in inputs/obj_det_vid.
'''

import boto3
import os
s3 = boto3.client('s3')
response = s3.list_objects_v2(Bucket='apollo-source-data',
                              Prefix='inputs/obj_det_vid')

paths = []
for content in response['Contents']:
    if content['Size'] != 0:
        paths.append(content['Key'])

for path in paths:
    cmd = ('curl localhost:8080/jobs/object_detection_vid/ -H \'Content-Type:application/json\' -X POST -d \'{"path":"%s"}\'' %(path))
    os.system(cmd)