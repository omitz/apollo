#!/bin/bash
#
# This script creates
# aars/datafeed-api-library.aar which is
# used by other projects such as APOLLO/Edge/atak/atak-helloworld-with-data-feed
#
set -e

# 1. Build the aar
./gradlew --no-daemon clean depend
./gradlew --console plain --no-daemon :main-lib:assembleRelease

# 2. publish as local Maven package at "speaker-id-library/build/repo/"
./gradlew --console=verbose :main-lib:publishMavenPublicationToMavenRepository

# # 3. Update the aar file
# AAR=$(ls -1rt main-lib/build/outputs/aar/*  | tail -1)
# ABSAAR=$(readlink -f $AAR)
# AARname=$(basename $AAR)
# cd aars/
# rm -f $AARname
# rm -f datafeed-api-library.aar
# RELAAR=$(realpath --relative-to=./ $ABSAAR)
# ln -s $RELAAR ./
# ln -s $AARname datafeed-api-library.aar
# cd -

# 4. Clean up
echo "maven repo symlink at aars/repo"

