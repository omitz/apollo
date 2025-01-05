# Swagger Doc

http://localhost:8080/swagger/

# Integration test

```
cd tests/
./integration_test.sh
```

# Manual testings

## 1.) Build docker image and run it
```
cd APOLLO/Command/
source .env

aws ecr get-login-password --region us-east-1 | docker login --password-stdin --username AWS 604877064041.dkr.ecr.us-east-1.amazonaws.com

docker-compose build flask-enrollment-backend
docker-compose up flask-enrollment-backend
```


## 2.) Test REST API 

Obtain a token:
```
export AUTH_TOKEN=$(curl -s -X POST -H 'Content-Type:application/json' -d '{"username":"john user", "password":"johnpassword"}' localhost:8080/login/ | grep authorization_token | sed -e s'/"authorization_token": "\(.*\)"/\1/')
```

```
# VIP tests
## set either faceID for speakerID
BASEURL="http://localhost:8080/createmodel/vip/?analytic=faceid&user=Wole"
BASEURL="http://localhost:8080/createmodel/vip/?analytic=speakerid&user=Wole"
AUTH_TOKEN=$(curl -s -X POST -H 'Content-Type:application/json' -d '{"username":"john user", "password":"johnpassword"}' localhost:8080/login/ | grep authorization_token | sed -e s'/"authorization_token": "\(.*\)"/\1/')

## list all vips
curl -H "authorization: Bearer $AUTH_TOKEN" "${BASEURL}"

## list all data samples in a specific vip
curl "${BASEURL}&vip=Jim_Gaffigan"

## list a specific data samples in a specific vip
curl "${BASEURL}&vip=Jim_Gaffigan&file=profile.jpg"

## add a new data samples to a specific vip
curl -X PUT -F file=@jim_gaffigan.jpg "${BASEURL}&vip=Jim_Gaffigan"

## remove an old data samples to a specific vip
curl -X DELETE "${BASEURL}&vip=Jim_Gaffigan&file=jim_gaffigan.jpg"

## remove a specific vip entirely
curl -X DELETE "${BASEURL}&vip=Jim_Gaffigan"

## vip-related download tests
### set either faceID for speakerID
BASEURL="http://localhost:8080/createmodel/vip/download/?analytic=faceid&user=Wole"
BASEURL="http://localhost:8080/createmodel/vip/download/?analytic=speakerid&user=Wole"

### download all vips
curl "${BASEURL}" -o all_vips.zip

### download specific vip
curl "${BASEURL}"&vip=Jim_Gaffigan -o Jim_Gaffigan_data.zip

### download specific data file in vip
curl "${BASEURL}"&vip=Jim_Gaffigan&file=profile.jpg -o Jim_Gaffigan_profile.jpg


# MISSION tests
# set either faceID for speakerID
BASEURL="http://localhost:8080/createmodel/mission/?analytic=faceid&user=Wole"
BASEURL="http://localhost:8080/createmodel/mission/?analytic=speakerid&user=Wole"

## list all missions
curl "${BASEURL}"

## list all vips in a specific mission
curl "${BASEURL}&mission=celebrity10"

## Train the mission
curl -X POST "${BASEURL}&mission=celebrity10"

## Add vip to the mission
curl -X PUT "${BASEURL}&vip=Jim_Gaffigan&mission=celebrity10"

## Delete a vip from the mission
curl -X DELETE "${BASEURL}&vip=Jim_Gaffigan&mission=celebrity10"

## Delete the entire mission
curl -X DELETE "${BASEURL}&mission=celebrity10"

## mission-related uplicate tests
### set either faceID for speakerID
BASEURL="http://localhost:8080/createmodel/mission/duplicate/?analytic=faceid&user=Wole"
BASEURL="http://localhost:8080/createmodel/mission/duplcate/?analytic=speakerid&user=Wole"
## Duplicate the entire mission
curl -X PUT "${BASEURL}&old-mission=celebrity10&new-mission=celebrity10_v2"

## mission-related download tests
### set either faceID for speakerID
BASEURL="http://localhost:8080/createmodel/mission/download/?analytic=faceid&user=Wole"
BASEURL="http://localhost:8080/createmodel/mission/download/?analytic=speakerid&user=Wole"

## download atak data package for a specific mission
curl -s --head "${BASEURL}&mission=celebrity10&download=atak"
curl "${BASEURL}&mission=celebrity10&download=atak" -o data_package.zip

## download a specific mission dataset
curl "${BASEURL}&mission=celebrity10&download=dataset" -o dataset.zip

# MISC tests
# set either faceID for speakerID
BASEURL="http://localhost:8080/createmodel/misc/?analytic=faceid&user=Wole"
BASEURL="http://localhost:8080/createmodel/misc/?analytic=speakerid&user=Wole"

## get filtered logs
curl -s "${BASEURL}&filter-user=Wole"

## remove cache
curl -X DELETE "${BASEURL}"

```



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
docker exec -it command_flask-enrollment-backend_1 /bin/bash
docker exec -it command_postgres_1 /bin/bash
psql -U postgres apollo
```

## How are concurrency and race condition handled? 
- Training can only start when no operation of the same analytic is
  currently running. 
  - This is acheived by using a global thread mutex lock for each analytic
  
- While training a specific mission, the following operation are not allowed:
  1. Download atak data package for the same mission
  2. Do another training of the same mission
  3. Any deletion within the same mission
  4. Any deletion within vips that are enrolled in the same mission
  5. Any addition within the same mission

