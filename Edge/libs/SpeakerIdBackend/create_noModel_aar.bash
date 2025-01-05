#!/bin/bash

set -e

# we just put nothing in the classifier/ folder (except README.md) 
CLASSIFIER_DIR=speaker-id-library/src/main/assets/syncSpkrID/classifier/
(find $CLASSIFIER_DIR -type f | grep -v README.md | xargs rm ) 2> /dev/null 

# don't forget the profile pics
rm speaker-id-library/src/main/assets/syncSpkrID/profiles.zip*

# We also update assets.lst 
ASSETS_LST=speaker-id-library/src/main/assets/syncSpkrID/assets.lst
sed -i '\|classifier/meta_vosk.pkl|d' $ASSETS_LST
sed -i '\|classifier/svm_vosk.json|d' $ASSETS_LST
sed -i '\|profiles.zip|d' $ASSETS_LST

# Now, we simply repackage AAR
\cp -f speaker-id-library/build.gradle.fataar  speaker-id-library/build.gradle
\rm speaker-id-library/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --no-daemon :speaker-id-library:assembleRelease
AAR=$(ls -1rt speaker-id-library/build/outputs/aar/*release.aar  | tail -1)

# And copy to S3 bucket
destAAR=$(basename "$AAR")
destAAR=${destAAR//.aar/_noModel.aar}   # append '_noModel' to aar file name
\cp $AAR ~/mnt/s3Atak/atak/speakerID_aar/$destAAR

# Now, we retore the assets
git checkout speaker-id-library/src/main/assets/syncSpkrID/

# we are done
echo "$AAR copied to ~/mnt/s3Atak/atak/speakerID_aar/$destAAR"
