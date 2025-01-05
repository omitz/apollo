#!/bin/bash
set -e

## build apolloedge
cd helloworld/
#./gradlew --no-daemon clean depend
./gradlew --no-daemon packCivDebug
cd -
APK=$(ls -1rt helloworld/app/build/outputs/apk/civ/debug/*.apk  | tail -1)
mv $APK atak_helloworld_with_speakerId_debug.apk

##
echo "To intall apollo plugin, do:"
echo "  adb install atak_helloworld_with_speakerId_debug.apk"
