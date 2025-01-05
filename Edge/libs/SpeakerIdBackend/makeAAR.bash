#!/bin/bash
set -e

# 1. Use the right build.gradle
\cp -f speaker-id-library/build.gradle.fataar  speaker-id-library/build.gradle


# 2. Build the aar
\rm speaker-id-library/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --no-daemon :speaker-id-library:assembleRelease


# 3. Copy the aar
AAR=$(ls -1rt speaker-id-library/build/outputs/aar/*release.aar  | tail -1)
\cp $AAR ./
echo "AAR created:  `basename $AAR`"
