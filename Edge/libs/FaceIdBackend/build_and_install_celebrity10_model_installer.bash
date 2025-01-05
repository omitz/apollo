#!/bin/bash
set -e

# 0. catch control-c to kill logcat
function func_trap()
{
    echo "killing pid $logcat_pid"
    kill $logcat_pid
}

trap func_trap INT ERR


# 1. Compile the app
./gradlew --console plain --no-daemon :faceid-celebrity10-model-installer:assembleDebug


# 2. Install the app
adb uninstall com.caci.apollo.faceid_celebrity10_model_installer 2> /dev/null || true
adb install faceid-celebrity10-model-installer/build/outputs/apk/debug/faceid-celebrity10-model-installer-debug.apk




# 3. Run the app
echo "Please manually accept storage permssion..."
## monitor logcat (optional)
adb logcat -c
adb logcat | grep Tommy --color=always &
logcat_pid=$(jobs -p | tail -1)

## Start the app
adb shell am start --user 0 -n com.caci.apollo.faceid_celebrity10_model_installer/.MainActivity


##  terminate logcat
echo "logcat pid = $logcat_pid"
#kill $logcat_pid
echo "control-c to exit logcat"
wait
