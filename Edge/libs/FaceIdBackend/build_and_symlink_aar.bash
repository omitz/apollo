#!/bin/bash
#
# This script creates
# aars/apollo-faceId-library-debug/apollo-faceId-library.aar which is
# used by other projects such as APOLLO/Edge/atak/atak-helloworld-with-face-id
#
set -e

# 1. Use the right build.gradle
\cp -f face-id-library/build.gradle.fataar  face-id-library/build.gradle

# 2. Build the aar
\rm face-id-library/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --no-daemon :face-id-library:assembleRelease

# 3. Update the aar file
AAR=$(ls -1rt face-id-library/build/outputs/aar/*  | tail -1)
ABSAAR=$(readlink -f $AAR)
AARname=$(basename $AAR)
cd aars/apollo-faceId-library-debug/
rm -f $AARname
rm -f apollo-faceId-library.aar
RELAAR=$(realpath --relative-to=./ $ABSAAR)
ln -s $RELAAR ./
ln -s $AARname apollo-faceId-library.aar
cd -

# 4. Clean up
echo "Created symlink aars/apollo-faceId-library-debug/apollo-faceId-library.aar"
