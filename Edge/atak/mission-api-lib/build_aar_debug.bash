#!/bin/bash
set -e

## build mission-api-libary as an AAR
./gradlew --no-daemon clean depend
./gradlew --console plain --no-daemon :lib:assembleCivDebug
APK=$(ls -1rt lib/build/outputs/aar/*.aar  | tail -1)
mv $APK mission-api-lib-debug.aar

##
echo "  created mission-api-lib-debug.aar"
