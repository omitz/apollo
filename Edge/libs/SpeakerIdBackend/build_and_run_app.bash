#!/bin/bash
set -e

# 1. Use the right build.gradle
#\cp -f speaker-id-library/build.gradle.fataar  speaker-id-library/build.gradle

# 2. Build the aar
\rm speaker-id-library/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --no-daemon :speaker-id-library:assembleRelease

#3. publish as local Maven package at "speaker-id-library/build/repo/"
./gradlew --console=verbose :speaker-id-library:publishMavenPublicationToMavenRepository

# 4. Compile the app
./gradlew --no-daemon :app:assembleDebug

# 5. Install the app
adb uninstall com.caci.apollo.speakeridbackend  || true
adb install ./app/build/outputs/apk/debug/app-debug.apk

# 6. Run the app
# adb logcat -c
# adb logcat &
# logcat_pid=$(jobs -p | tail -1)

adb shell am start --user 0 -n com.caci.apollo.speakeridbackend/.MainActivity

# sleep 10
# echo "logcat pid = $logcat_pid"
# kill $logcat_pid
