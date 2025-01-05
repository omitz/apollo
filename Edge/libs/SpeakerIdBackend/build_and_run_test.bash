#!/bin/bash
set -e

# 0. catch control-c to kill logcat
function func_trap()
{
    echo "killing pid $logcat_pid"
    kill $logcat_pid
}

trap func_trap INT ERR


# 1. check commandline parameters
if ! [ $# -eq 1 ]; then
    cat <<EOF 
Usage:
  $0 <parameter> 

Where <parameter> could be one of 
   'assets':  Load classifier model from assets 
   'data_package': Load classifier model from ATAK data package 
                   (assume be installed imported already)
Example:
  $0 assets
  $0 data_package
EOF
    exit 0;
fi
ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)
PARAM=$1




# 1. Use the right build.gradle
#\cp speaker-id-library/build.gradle.module  speaker-id-library/build.gradle

## 2. monitor logcat (optional)
adb logcat -c
adb logcat | grep Tommy --color=always &
logcat_pid=$(jobs -p | tail -1)

# 3. Run the instrumented test
if [[ "$PARAM" == "data_package" ]]; then
    ## Fake an import data package action:
    adb push data-package/ApolloSpeakerID-10VIP.zip /sdcard/download/
    cmd0="mkdir -p /sdcard/atak/tools/datapackage/files/"
    cmd1="cd /sdcard/atak/tools/datapackage/files/"
    cmd2="mkdir -p ApolloSpeakerID-10VIP"
    cmd3="yes | unzip -d ApolloSpeakerID-10VIP /sdcard/download/ApolloSpeakerID-10VIP.zip"
    adb shell "$cmd0; $cmd1; $cmd2; $cmd3"
fi
./gradlew --console plain speaker-id-library:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.caci.apollo.speaker_id_library.SpeakerIdTest#test1 -PTEST_PARAM1=$PARAM || true


# 3.5 Restore  build.gradle
#\cp speaker-id-library/build.gradle.fataar  speaker-id-library/build.gradle

## 4. terminate logcat
echo "logcat pid = $logcat_pid"
kill $logcat_pid
