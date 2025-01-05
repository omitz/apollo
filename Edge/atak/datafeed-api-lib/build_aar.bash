#!/bin/bash
set -e

## build datafeed-api-libary as an AAR
./gradlew --no-daemon clean depend
./gradlew --console plain --no-daemon :main-lib:assembleRelease
#./gradlew --console plain --no-daemon :main-lib:assembleDebug
APK=$(ls -1rt main-lib/build/outputs/aar/*.aar  | tail -1)
mv $APK datafeed-api-library-release.aar

##
echo "  created datafeed-api-library-release.aar"


#./gradlew --console plain --no-daemon :helloworld:assembleCivRelease
#APK=$(ls -1rt helloworld/build/outputs/apk/civ/release/*.apk | tail -1)
#mv $APK helloworld-release.apk
#echo "  created helloworld"
