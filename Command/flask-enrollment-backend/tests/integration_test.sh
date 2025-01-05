#!/usr/bin/env bash

set -ue
PROG_DIR=$(dirname $(readlink -f "$0"))
EXEC_DIR=$(pwd -P)
MY_NAME=`basename $0`

function err_report {
   echo "Fail @ [${MY_NAME}:${1}]"
   exit 1
}

trap 'err_report $LINENO' ERR

function add_delay {
    DELAY=$1
    trap 'err_report $LINENO' ERR
    
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
    trap 'err_report $LINENO' ERR

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
    trap 'err_report $LINENO' ERR

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
    trap 'err_report $LINENO' ERR

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

function wait_until_flask_ready()
{
    flask=$1
    trap 'err_report $LINENO' ERR
    
    count=0
    while (! docker-compose logs $flask | grep Running); do
        echo "$count: Waiting for $flask Running"
        count=$((count + 1))
        sleep 1
    done
}


function parseJson()
{
    trap 'err_report $LINENO' ERR
    key=$1
    python -c "import json,sys;obj=json.load(sys.stdin);print(obj['$key']);"
}

function version_greater_equal()
{
    trap 'err_report $LINENO' ERR
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
cd $PROG_DIR/../../

# Start everything from scratch
docker-compose down

export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1
services="flask-enrollment-backend"
docker-compose build --force-rm $services

# Set env variables our helper scripts will use
set -a
source .env
set +a

# Bring up the services
docker-compose up -d --remove-orphan $services
wait_until_flask_ready flask-enrollment-backend

# Done with starting up services, clean up
docker rmi $(docker images --filter "dangling=true" -q --no-trunc) || true
cd -

# Run tests
function TestVIP () {
    ##   addFile = "jim_gaffigan.jpg" or "jim_gaffigan.aac"
    if [[ ! $# -eq 2 ]]; then
        exit 1
    fi
    local addFile="$1"
    local addBadFile="$2"
    trap 'err_report $LINENO' ERR

    BASEURL="http://localhost:8080/createmodel/vip/?analytic=${ANALYTIC}&user=Wole"

    ### GET
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Querying all VIPs:"
    echo -e "-------------------------------"
    curlTok -s "${BASEURL}" | grep '"success": true'
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Querying specifc VIP:"
    echo -e "-------------------------------"
    curlTok -s "${BASEURL}&vip=Jim_Gaffigan" | grep '"success": true'
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Querying specifc VIP file:"
    echo -e "-------------------------------"
    curlTok -s "${BASEURL}&vip=Jim_Gaffigan&file=profile.jpg" | grep '"success": true'
    echo
    ### PUT
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Add a new file to a specifc VIP:"
    echo -e "-------------------------------"
    curlTok -s -X DELETE "${BASEURL}&vip=Jim_Gaffigan&file=$addFile"
    curlTok -s -X PUT -F file=@${addFile} "${BASEURL}&vip=Jim_Gaffigan" | grep '"success": true'
    curlTok -s -X DELETE "${BASEURL}&vip=Jim_Gaffigan&file=$addBadFile"
    curlTok -s -X PUT -F file=@${addBadFile} "${BASEURL}&vip=Jim_Gaffigan" | grep '"success": false'
    echo
    ### DELETE
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Remove a file from specific VIP:"
    echo -e "-------------------------------"
    curlTok -s -X DELETE "${BASEURL}&vip=Jim_Gaffigan&file=$addFile" | grep '"success": true'
    curlTok -s -X DELETE "${BASEURL}&vip=Jim_Gaffigan&file=$addBadFile" | grep '"success": false'
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Remove a specific VIP entirely:"
    echo -e "-------------------------------"
    curlTok -s -X DELETE "${BASEURL}&vip=Jim_Gaffigan" | grep '"success": true'
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Restore the specific VIP :"
    echo -e "-------------------------------"
    git checkout ../api/FaceIdTraining/vips_with_profile_picture/
    git checkout ../api/SpeakerIdTraining/vips_with_profile_picture/
    git checkout ../api/FaceIdTraining/missions/
    git checkout ../api/SpeakerIdTraining/missions/
}


function TestMission () {
    trap 'err_report $LINENO' ERR

    BASEURL="http://localhost:8080/createmodel/mission/?analytic=${ANALYTIC}&user=Wole"

    ### GET
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Querying all missions:"
    echo -e "-------------------------------"
    curlTok -s "${BASEURL}" | grep '"success": true'
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Querying specifc mission:"
    echo -e "-------------------------------"
    curlTok -s "${BASEURL}&mission=celebrity10" | grep '"success": true'
    echo
    ### PUT
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Add a new VIP to a specifc mission:"
    echo -e "-------------------------------"
    curlTok -s -X PUT "${BASEURL}&mission=newMission&vip=Jim_Gaffigan" | grep '"success": true'
    echo
    ### POST
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Training celebrity10 enrollment...(takes 40 sec to 2 min):"
    echo -e "-------------------------------"
    time curlTok -s -X POST "${BASEURL}&mission=celebrity10" | grep '"success": true'
    echo
    ### DELETE
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Remove a vip from specific mission:"
    echo -e "-------------------------------"
    curlTok -s -X DELETE "${BASEURL}&vip=Jim_Gaffigan&mission=newMission" | grep '"success": true'
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Remove  mission:"
    echo -e "-------------------------------"
    curlTok -s -X DELETE "${BASEURL}&mission=newMission" | grep '"success": true'
}


function TestDownloads () {
    trap 'err_report $LINENO' ERR

    BASEURL="http://localhost:8080/createmodel/mission/download/?analytic=${ANALYTIC}&user=Wole"

    ### GET
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Downloading celebrity10 atak data pacakge:"
    echo -e "-------------------------------"
    # atak file name from the server can be retrieved using the
    # Content-Disposition header information.  The name of the data
    # package is "atak_uuid=<uuid>.zip".
    atakPkgName=$(curlTok -s -I "${BASEURL}&mission=celebrity10&download=atak" \
                      | grep filename | tr -d '\r' | sed 's/.*filename=//' \
                      | tr -d '"') 
    echo "atakPkgName = '${atakPkgName}'"
    # download the file specified by the server.  But it does not overwrite.
    # curlTok -sJ "${BASEURL}&enrollment=celebrity10&download=atak"
    curlTok -s "${BASEURL}&mission=celebrity10&download=atak" -o ${atakPkgName}
    file ${atakPkgName} | grep Zip
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Downloading celebrity10 performance evaluation:"
    echo -e "-------------------------------"
    # preformance file name from the server can be retrieved using the
    # Content-Disposition header information.  
    perPdfName=$(curlTok -s -I "${BASEURL}&mission=celebrity10&download=performance" \
                      | grep filename | tr -d '\r' | sed 's/.*filename=//' \
                      | tr -d '"') 
    echo "perPdfName = '${perPdfName}'"
    # download the file specified by the server. But it does not overwrite.
    # curlTok -sJ "${BASEURL}&enrollment=celebrity10&download=performance"
    curlTok -s "${BASEURL}&mission=celebrity10&download=performance" -o ${perPdfName}
    file ${perPdfName} | grep PDF
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Downloading celebrity10 dataset:"
    echo -e "-------------------------------"
    dataSetZip=${ANALYTIC}_celebrity10.zip
    curlTok -s "${BASEURL}&mission=celebrity10&download=dataset" -o $dataSetZip \
        && (file $dataSetZip | grep Zip)
    echo


    BASEURL="http://localhost:8080/createmodel/vip/download/?analytic=${ANALYTIC}&user=Wole"

    ### GET
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Downloading specific vip data:"
    echo -e "-------------------------------"
    dataSetZip=${ANALYTIC}-Jim_Gaffigan.zip
    curlTok -s "${BASEURL}&vip=Jim_Gaffigan" -o $dataSetZip && (file $dataSetZip | grep Zip)
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Downloading all vips:"
    echo -e "-------------------------------"
    dataSetZip=${ANALYTIC}-VIPs.zip
    curlTok -s "${BASEURL}" -o $dataSetZip && (file $dataSetZip | grep Zip)
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Downloading specific file:"
    echo -e "-------------------------------"
    dataFile=${ANALYTIC}-Jim_Gaffigan-profile.jpg
    curlTok -s "${BASEURL}&vip=Jim_Gaffigan&file=profile.jpg" -o ${dataFile}
    file $dataFile | grep JPEG
    echo

}


function TestDuplicate () {
    trap 'err_report $LINENO' ERR

    BASEURL="http://localhost:8080/createmodel/mission/duplicate/?analytic=${ANALYTIC}&user=Wole"

    ### PUT
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Duplicate a specific mission:"
    echo -e "-------------------------------"
    curlTok -s -X PUT "${BASEURL}&old-mission=celebrity10&new-mission=celebrity10_V2" \
        | grep '"success": true'
    echo

    ### 

    BASEURL="http://localhost:8080/createmodel/mission/?analytic=${ANALYTIC}&user=Wole"
    echo
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Remove  mission:"
    echo -e "-------------------------------"
    curlTok -s -X DELETE "${BASEURL}&mission=celebrity10_V2" | grep '"success": true'

}


function TestMisc () {
    trap 'err_report $LINENO' ERR

    BASEURL="http://localhost:8080/createmodel/misc/?analytic=${ANALYTIC}&user=Wole"

    ### GET
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Remove cache:"
    echo -e "-------------------------------"
    curlTok -s "${BASEURL}&filter-user=Wole" | grep '"success": true'
    echo
    
    ### DELETE
    echo -e "-------------------------------"
    echo -e "  * ${MODULE}: Remove cache:"
    echo -e "-------------------------------"
    curlTok -s -X DELETE "${BASEURL}" | grep '"success": true'
    echo
}

##################################
## Get security token
##################################
export AUTH_TOKEN=`curl -s -X POST -H 'Content-Type:application/json' -d '{"username":"john user", "password":"johnpassword"}' localhost:8080/login/ | grep authorization_token | sed -e s'/"authorization_token": "\(.*\)"/\1/'`
function curlTok () {
    curl -H "authorization: Bearer $AUTH_TOKEN" "$@"
}

##################################
## Exercise FaceID functions
##################################
echo "********* TESTING FACEID ************"
MODULE="FACEID"
ANALYTIC="faceid"
TestVIP jim_gaffigan.jpg smaller_img.jpg
TestMission
TestDownloads
TestDuplicate
TestMisc

##################################
## Exercise SpeakerID functions
##################################
echo "********* TESTING SPEAKERID ************"
MODULE="SPEAKERID"
ANALYTIC="speakerid"
TestVIP jim_gaffigan.aac short_test.m4a
TestMission 
TestDownloads
TestDuplicate
TestMisc

# Success!
echo ""
echo "Enrollment Integration test succeeded!"
