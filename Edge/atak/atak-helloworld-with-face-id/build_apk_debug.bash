#!/bin/bash
set -e

## build apolloedge
cd helloworld/
./gradlew --no-daemon clean depend
./gradlew --console plain --no-daemon packCivDebug
cd -
APK=$(ls -1rt helloworld/app/build/outputs/apk/civ/debug/*.apk  | tail -1)
mv $APK atak_helloworld_with_faceId.apk

##
echo "To intall apollo plugin, do:"
echo "  adb uninstall com.atakmap.android.helloworld.plugin"
echo "  adb install atak_helloworld_with_faceId.apk"
