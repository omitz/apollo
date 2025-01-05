#!/bin/bash

set -e

# we just put nothing in the classifier/ folder (except README.md)
\rm face-id-library/src/main/assets/model
\rm face-id-library/src/main/assets/label
\rm face-id-library/src/main/assets/profiles.zip

# Now, we simply repackage AAR
\cp -f face-id-library/build.gradle.fataar  face-id-library/build.gradle
\rm face-id-library/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --no-daemon :face-id-library:assembleRelease
AAR=$(ls -1rt face-id-library/build/outputs/aar/*release.aar  | tail -1)

# And copy to S3 bucket
destAAR=$(basename "$AAR")
destAAR=${destAAR//.aar/_noModel.aar}   # append '_noModel' to aar file name
\cp $AAR ~/mnt/s3Atak/atak/faceID_aar/$destAAR

# Now, we retore the assets
git checkout face-id-library/src/main/assets/

# clean ups
echo "$AAR copied to ~/mnt/s3Atak/atak/faceID_aar/$destAAR"
