# Replace the respective files in apollo_edge_app/app/src/main/assets/face with the new data, label, and model files.
adb pull /storage/emulated/0/facerecognizer/data ../../../../../../../assets/face/
adb pull /storage/emulated/0/facerecognizer/label ../../../../../../../assets/face/
adb pull /storage/emulated/0/facerecognizer/model ../../../../../../../assets/face/
echo "post-retraining steps complete."