#!/bin/bash
set -e

# 1. Compile the app
./gradlew --console plain --no-daemon :speakerid-celebrity10-model-installer:assembleDebug

# 2. Copy the installer apk
cp speakerid-celebrity10-model-installer/build/outputs/apk/debug/speakerid-celebrity10-model-installer-debug.apk speakerid-celebrity10-model-installer.apk 

# 3. Clean ups
echo "created speakerid-celebrity10-model-installer.apk"
echo "To install, do:"
echo "   adb install speakerid-celebrity10-model-installer.apk"
echo "   You must then run the installer to install the model"
