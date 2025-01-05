#!/bin/bash
set -e

# 1. Use the right build.gradle
\cp -f face-id-library/build.gradle.fataar  face-id-library/build.gradle

# 2. Build the aar
\rm face-id-library/build/outputs/aar/*  || true
./gradlew --no-daemon clean depend
./gradlew --no-daemon :face-id-library:assembleDebug

# 3. Update the aar file
AAR=$(ls -1rt face-id-library/build/outputs/aar/*  | tail -1)
ABSAAR=$(readlink -f $AAR)
#\cp $AAR aars/apollo-faceId-library-debug/apollo-faceId-library-debug.aar
AARname=$(basename $AAR)
# \cp $AAR aars/apollo-faceId-library-debug/
# rm aars/apollo-faceId-library-debug/apollo-faceId-library-debug.aar
cd aars/apollo-faceId-library-debug/
rm -f $AARname
rm -f apollo-faceId-library-debug.aar
RELAAR=$(realpath --relative-to=./ $ABSAAR)
ln -s $RELAAR ./
ln -s $AARname apollo-faceId-library-debug.aar
cd -

# 4. Compile the app
./gradlew --console plain --no-daemon :app:assembleDebug

# 5. Install the app
adb uninstall com.caci.apollo.faceidbackend  || true
adb install ./app/build/outputs/apk/debug/app-debug.apk

# 6. Run the app
adb shell am start --user 0 -n com.caci.apollo.faceidbackend/.MainActivity

