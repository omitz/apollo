#!/bin/bash
set -e

## build mission-api-libary as an AAR
./gradlew --no-daemon clean depend
./gradlew --console plain --no-daemon :lib:assembleCivRelease
APK=$(ls -1rt lib/build/outputs/aar/*.aar  | tail -1)
mv $APK mission-api-lib-release.aar

##
echo "  created mission-api-lib-release.aar"
