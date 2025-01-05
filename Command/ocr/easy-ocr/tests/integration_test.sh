#!/usr/bin/env bash

set -ue
MY_NAME=`basename $0`
EXEDIR=$(dirname $0)

function err_report
{
   echo "Fail @ [${MY_NAME}:${1}]"
   exit 1
}

trap 'err_report $LINENO' ERR

function add_delay
{
    DELAY=$1
    for idx in $(seq $DELAY); do
        X=$(printf '=%.0s' $(seq $idx))
        printf "Adding Delay: %-${DELAY}s   [ ${idx}/${DELAY} ]\r" $X
        sleep 1
    done
    echo
}

function wait_until_rabbitmq_connections()
{
    numConnections=$1
    cmd="docker-compose exec rabbitmq rabbitmqctl list_connections | grep running | wc -l"
    count=0
    
    while (! docker-compose exec rabbitmq rabbitmqctl list_connections > /dev/null); do
        echo "$count: Waiting for rabbitmq to start."
        count=$((count + 1))
        sleep 1
    done
    
    while (! test $(eval "$(echo $cmd)") -ge $numConnections); do
        echo "$count: Waiting for >= $numConnections rabbitmq connections."
        count=$((count + 1))
        sleep 1
    done
}

function wait_until_database_path_ready()
{
    model=$1
    path=$2

    count=0
    cmd="curl -s 'localhost:8080/search/check_database?model=${model}&path=${path}'"
    cmd="$cmd -H 'authorization: Bearer $token'"
    while (! test $(eval "$(echo $cmd)") == "true"); do
        echo "$count: Waiting for database model='$model', path='$path'."
        count=$((count + 1))
        sleep 1
    done
}

function wait_until_database_ready()
{
    model=$1
    nData=$2

    count=0
    cmd="curl -s 'localhost:8080/search/check_database?model=${model}'"
    cmd="$cmd -H 'authorization: Bearer $token'"
    INTEGER=$(eval "$(echo $cmd)")   # catch error
    test $INTEGER -eq $INTEGER       #  ..
    while (test 0$(eval "$(echo $cmd)") -lt $nData); do
        echo "$count: Waiting for database model='$model' nData >= $nData"
        count=$((count + 1))
        sleep 1
    done
}

function wait_until_ner_ready()
{
    nData=$1

    count=0
    cmd="curl -s http://localhost:8084/size"
    INTEGER=$(eval "$(echo $cmd)")   # catch error
    test $INTEGER -eq $INTEGER       #  ..
    while (test 0$(eval "$(echo $cmd)") -lt $nData); do
        echo "$count: Waiting for ner nData >= $nData"
        count=$((count + 1))
        sleep 1
    done
}

function parseJson()
{
    key=$1
    python3 -c "import json,sys;obj=json.load(sys.stdin);print(obj['$key']);"
}

function urlencode()
{
    str=$1
    python3 -c "import sys, urllib.parse as ul; print (ul.quote_plus('$str'));"
}

function urldecode()
{
    str=$1
    python3 -c "import sys, urllib.parse as ul; print(ul.unquote_plus('$str'))"
}

function version_greater_equal()
{
    printf '%s\n%s\n' "$2" "$1" | sort -V -C
}

dockerComposeVersion=$(docker-compose version --short)
if (version_greater_equal "1.25.1" "${dockerComposeVersion}"); then
    echo
    echo "WARNING -- docker-compose ${dockerComposeVersion} is too slow."
    echo "           Plesae upgrade to > 1.25.1"
    echo
    exit -1
fi

# Run from apollo/Command
cd $EXEDIR/../../..

# Start everything from scratch
docker-compose down

export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1 
services="flask-apollo-processor ocr-easy"
services="$services named-entity-recognition named-entity-recognition-search"
docker-compose build $services

# Set env variables our helper scripts will use
set -a
source .env
set +a

# Bring up the services
docker-compose up -d $services
wait_until_rabbitmq_connections 2

# Get Authentication token
token=$(curl -s -X POST -H "Content-type: application/json" \
     -d '{"username":"john user", "password":"johnpassword"}' localhost:8080/login/ | \
     parseJson authorization_token)

# We can send an image file:
curl -s -X POST localhost:8080/jobs/ocr_easy/ \
     -H "authorization: Bearer $token" \
     -H 'Content-Type:application/json' -d '{"path": "inputs/ocr/eng_paragraphs.png"}'

# We can send an image file:
curl -s -X POST localhost:8080/jobs/ocr_easy/ \
     -H "authorization: Bearer $token" \
     -H 'Content-Type:application/json' -d '{"path": "inputs/ocr/arabic.png"}'

# Let the job finish
wait_until_database_ready "SearchFullText" 2
wait_until_ner_ready 18

# search "Sue & Malden" 
curl -s localhost:8080/search/full_text?query=Sue+%26+Malden \
     -H "authorization: Bearer $token" \
     | grep ocr-easy


# search arabic مكتبة  (Library)
entity_encode=$(urlencode مكتبة)
curl -s "localhost:8080/search/full_text?query=$entity_encode" \
     -H "authorization: Bearer $token" \
     | grep ocr-easy

# search "Sue Malden" in NER:
curl -s http://localhost:8080/search/ner?entity=Sue+Malden \
     -H "authorization: Bearer $token" \
     | grep ocr-easy

# Success!
echo "Apollo Integration test succeeded!"
