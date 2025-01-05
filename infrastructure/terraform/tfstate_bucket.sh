#!bin/bash

aws_region=us-east-1
aws_profile=

tfstate_name=

tfstate_s3_bucket=${tfstate_name}
tfstate_dynamodb_table=${tfstate_name}

aws s3 mb s3://${tfstate_s3_bucket} \
    --region "${aws_region}" \
    --profile "${aws_profile}"

aws s3api put-bucket-versioning \
    --region "${aws_region}" \
    --profile "${aws_profile}" \
    --bucket "${tfstate_s3_bucket}" \
    --versioning-configuration "Status=Enabled"

aws dynamodb create-table \
    --region "${aws_region}" \
    --profile "${aws_profile}" \
    --table-name "${tfstate_dynamodb_table}" \
    --key-schema "AttributeName=LockID,KeyType=HASH" \
    --provisioned-throughput "ReadCapacityUnits=5,WriteCapacityUnits=5" \
    --attribute-definitions "AttributeName=LockID,AttributeType=S AttributeName=Digest,AttributeType=S"
