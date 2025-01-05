#!/bin/bash
#
# This script creates a local Maven report at aars/repo which.  It is
# used by other projects such as
# APOLLO/Edge/atak/atak-helloworld-with-speaker-id
#
set -e

# 1. Build the aar
./gradlew --no-daemon clean depend
./gradlew --no-daemon -console plain :speaker-id-library:assembleRelease

# 2. publish as local Maven package at "speaker-id-library/build/repo/"
./gradlew --console=verbose :speaker-id-library:publishMavenPublicationToMavenRepository

# 3. Clean up
echo "maven repo symlink at aars/repo"
