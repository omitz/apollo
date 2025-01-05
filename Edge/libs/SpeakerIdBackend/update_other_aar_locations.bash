#!/bin/bash

set -e

read -p "Are you sure (y/n)?"
echo "Needs fix"
exit 1
./make_javadoc.bash

## with with fataar build
\cp -f speaker-id-library/build.gradle.fataar  speaker-id-library/build.gradle
\rm speaker-id-library/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --no-daemon :speaker-id-library:assembleRelease
AAR=$(ls -1rt speaker-id-library/build/outputs/aar/*release.aar  | tail -1)
JAVADOC="apollo-speakerId-library-javadoc.jar"
# TARGET_AAR=$(basename $AAR | sed 's/-release//')


## Apollo Plugin
#\cp $AAR ../../atak/apolloedge/aars/speaker-id-library/speaker-id-library.aar


## Helloworld SpeakerID Plugin  (S3 bucket)
\cp $AAR ../../atak/atak-helloworld-with-speaker-id/helloworld/aars/speaker-id-library/speaker-id-library.aar
\cp $JAVADOC ~/mnt/s3Atak/atak/speakerID_v4/
\cp list_of_speakers.txt ~/mnt/s3Atak/atak/speakerID_v4/
cd ../../atak/atak-helloworld-with-speaker-id/
./makeTgz.bash atak_helloworld_with_speakerId_0p9p7.tgz
\cp atak_helloworld_with_speakerId_0p9p7.tgz ~/mnt/s3Atak/atak/speakerID_v4/
cd -


## speaker-ID aar library (S3 bucket)
#\rm ~/mnt/s3Atak/atak/speakerID_aar/*.aar
\cp $AAR ~/mnt/s3Atak/atak/speakerID_aar/
\cp ./speaker-id-library/src/androidTest/java/com/caci/apollo/speaker_id_library/SpeakerIdTest.java ~/mnt/s3Atak/atak/speakerID_aar/
\cp $JAVADOC ~/mnt/s3Atak/atak/speakerID_aar/
\cp list_of_speakers.txt ~/mnt/s3Atak/atak/speakerID_aar/
\cp ./speaker-id-library/src/androidTest/assets/Katie_Holmes.aac ~/mnt/s3Atak/atak/speakerID_aar/
\cp ./speaker-id-library/src/androidTest/assets/testSpkr_mono_16bit_16khz.wav ~/mnt/s3Atak/atak/speakerID_aar/


