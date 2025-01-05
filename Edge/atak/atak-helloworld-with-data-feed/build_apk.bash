#!/bin/bash
set -e

## build apolloedge
cd helloworld/
./gradlew --no-daemon clean depend
./gradlew --console plain --no-daemon app:assembleCivRelease
cd -
APK=$(ls -1rt helloworld/app/build/outputs/apk/civ/release/*.apk  | tail -1)
mv $APK atak_helloworld_with_speakerId_datafeed.apk

##
echo "To intall apollo plugin, do:"
echo "  adb uninstall com.atakmap.android.helloworld.plugin"
echo "  adb install atak_helloworld_with_speakerId_datafeed.apk"
