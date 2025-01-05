#!/bin/bash

set -e

cd aars/apollo-faceId-library
./rebuildSymlink.bash
cd -
./build_and_symlink_latest_speakerid_aar.bash
./build_and_symlink_latest_datafeed_aar.bash

./gradlew --no-daemon clean depend
./gradlew --no-daemon --console plain packCivDebug

rm -f apollo-edge*.apk
cp ./app/build/outputs/apk/civ/debug/*.apk .

echo "You can now install apk by:"
echo "   adb uninstall com.atakmap.android.apolloedge.plugin"
echo "   adb install apollo-edge-*-civ-debug.apk"


