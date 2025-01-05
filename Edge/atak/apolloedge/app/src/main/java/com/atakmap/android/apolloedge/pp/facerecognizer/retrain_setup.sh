VIPS_ON_PHONE=/storage/emulated/0/vips
# Delete any existing vips directory on the phone
adb shell rm -r $VIPS_ON_PHONE
# Get the vips path argument
VIPS_DIR_PATH="$1"
echo "VIPS_DIR_PATH:"
echo "$VIPS_DIR_PATH"
# Copy the new vips dataset to the phone:
adb push "$VIPS_DIR_PATH" $VIPS_ON_PHONE
# Delete the contents of assets/face/data and assets/face/label
LOCAL_DATA=../../../../../../../assets/face/data
LOCAL_LABELS=../../../../../../../assets/face/label
> $LOCAL_DATA
> $LOCAL_LABELS
# Uninstall and reinstall the application
adb uninstall com.atakmap.android.apolloedge.plugin
cd ../../../../../../../../../../
./gradlew --no-daemon packCivDebug
adb install ./app/build/outputs/apk/civ/debug/app-civ-debug.apk
echo "retrain setup complete."