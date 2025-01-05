#!/bin/bash
#
#
set -eu

read -p "Are you sure (y/n)?"

./make_javadoc.bash

#Use the right build.gradle
\cp -f face-id-library/build.gradle.fataar  face-id-library/build.gradle
#\cp -f face-id-library/build.gradle.module  face-id-library/build.gradle

\rm face-id-library/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --no-daemon :face-id-library:assembleRelease
AAR=$(ls -1rt face-id-library/build/outputs/aar/*release.aar  | tail -1)
JAVADOC="apollo-faceId-library-javadoc.jar"
# TARGET_AAR=$(basename $AAR | sed 's/-release//')

## Apollo Plugin
#\cp $AAR ../../atak/apolloedge/aars/apollo-faceId-library/apollo-faceId-library.aar

## GUI live camera app
\cp $AAR aars/apollo-faceId-library-debug/apollo-faceId-library-debug.aar

# ## GUI Still-image app
# \cp $AAR ../../AndroidCamera/aars/apollo-faceId-library-debug/apollo-faceId-library-debug.aar
# cd ../../AndroidCamera/
# ./makeTgz.bash
# \cp ../apollo_faceId_test_app.tgz  ~/mnt/s3Atak/atak/faceID/
# cd -
# \cp $JAVADOC ~/mnt/s3Atak/atak/faceID/
# \cp list_of_faces.txt ~/mnt/s3Atak/atak/faceID/


## Helloworld FaceID Plugin  (S3 bucket)
\cp $AAR ../../atak/atak-helloworld-with-face-id/helloworld/aars/apollo-faceId-library/apollo-faceId-library.aar
\cp $JAVADOC ~/mnt/s3Atak/atak/faceID_v2/
\cp list_of_faces.txt ~/mnt/s3Atak/atak/faceID_v2/
cd ../../atak/atak-helloworld-with-face-id/
./makeTgz.bash atak_helloworld_with_faceId_0p9p7.tgz
\cp atak_helloworld_with_faceId_0p9p7.tgz ~/mnt/s3Atak/atak/faceID_v2/
cd -


## face-ID aar library (S3 bucket)
\cp $AAR ~/mnt/s3Atak/atak/faceID_aar/
\cp ./face-id-library/src/androidTest/java/com/caci/apollo/face_id_library/FaceIdTest.java ~/mnt/s3Atak/atak/faceID_aar/
\cp $JAVADOC ~/mnt/s3Atak/atak/faceID_aar/
\cp list_of_faces.txt ~/mnt/s3Atak/atak/faceID_aar/
\cp ./face-id-library/src/androidTest/assets/face_demo_img.jpg ~/mnt/s3Atak/atak/faceID_aar/
\cp ./output.png ~/mnt/s3Atak/atak/faceID_aar/



