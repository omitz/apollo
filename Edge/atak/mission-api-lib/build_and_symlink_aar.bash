#!/bin/bash
#
# This script creates
# aars/mission-api-library.aar which is
# used by other projects such as APOLLO/Edge/atak/atak-helloworld-with-data-feed-v2
#
set -e

# 1. Use the right build.gradle
#\cp -f lib/build.gradle.fataar  lib/build.gradle

# 2. Build the aar
\rm lib/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --console plain --no-daemon :lib:assembleCivRelease
#./gradlew --console plain --no-daemon :lib:assembleCivDebug

# 3. Update the aar file
AAR=$(ls -1rt lib/build/outputs/aar/*  | tail -1)
ABSAAR=$(readlink -f $AAR)
AARname=$(basename $AAR)
cd aars/
rm -f $AARname
rm -f mission-api-library.aar
RELAAR=$(realpath --relative-to=./ $ABSAAR)
ln -s $RELAAR ./
ln -s $AARname mission-api-library.aar
cd -

# 4. Clean up
echo "Created symlink aars/mission-api-library.aar"
