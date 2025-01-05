# Manual testings

## 1.) Build docker image and run it

You may need to setup ECR:

```
aws ecr get-login-password --region us-east-1 | docker login --password-stdin --username AWS 604877064041.dkr.ecr.us-east-1.amazonaws.com
```

First build and run the enrollment backend

```
cd APOLLO/Command/
source .env
docker-compose build flask-enrollment-backend
docker-compose up flask-enrollment-backend
```

Then build and run Edge Enroll UI
```
cd APOLLO/Command/
source .env
docker-compose build edge-enroll-ui
docker-compose up edge-enroll-ui
```

Then access the Edge Enroll UI via
http://localhost:4000


# Reference and more Details
## What's in the APOLLO/Comman/.env file?
```
AWS_ACCESS_KEY_ID=`cat ~/.aws/credentials | grep key_id | awk '{print $3;}'`
AWS_SECRET_ACCESS_KEY=`cat ~/.aws/credentials | grep secret_ | awk '{print $3;}'`
BUCKET_NAME=apollo-source-data
RABBITMQ_HOST=rabbitmq
POSTGRES_HOST=postgres
POSTGRES_PASSWORD=secretpassword
POSTGRES_USER=postgres
DEBUG=True
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
MILVUS_HOST=milvus
NEO4J_HOST=neo4j
NEO4J_AUTH=neo4j/neo4j-password
ENDPOINT_ADMIN_USERNAME=admin
ENDPOINT_ADMIN_SECRET=cantaloupe-password
VUE_APP_ANALYTIC_ENV=local
FACE_SEARCH_HOST=facenet-search
NER_SEARCH_HOST=named-entity-recognition-search
LANDMARK_SEARCH_HOST=landmark-search
DEVELOPMENT=True
SPEAKER_SEARCH_HOST=speaker-search

export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1 
```

## How to attach to the docker instance?
```
docker exec -it command_edge-enroll-ui_1 /bin/bash
```
