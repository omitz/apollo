#!/bin/bash
#
# Train and create classifier model files for SpeakerID
#
# Also create an ATAK data package.  By convention, the name of UUID
# is "<uuid_prfix><enrollmenet>" and the zip file is "atak_uuid=<UUID>.zip"
#
# We assume that <enrollmenet> is the last directory in <enrollmenet_dir>
#
# TC 2021-09-28 (Tue) -- also create $OUTPUT_DIR/speakerid-3-fold-cross-validation.pdf
#
set -ue

ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)

if ! [ $# -eq 4 ]; then
    cat <<EOF 
Usage:
  $0 <enrollment_dir> <output_dir> <uuid_prefix> <max_audio_sec>

Example:
  $0 missions/celebrity10/ classifier_models/celebrity10/ SpeakerID_ 30

EOF
    exit -1;
fi
ENROLLMENT_DIR=$1
OUTPUT_DIR=$2
UUID_PREFIX=$3
MAX_AUDIO_SEC=$4

# Change to the directory where the excutables reside
cd $EXEDIR
mkdir -p $OUTPUT_DIR

# Train the Speaker classifier from scratch:
rm -f $OUTPUT_DIR/profiles.zip \
   $OUTPUT_DIR/meta_vosk.pkl \
   $OUTPUT_DIR/*.csv \
   $OUTPUT_DIR/svm_vosk.json
echo | ./create_dataset_lists.py --nTrainPerVIP -1 \
                                --nTestPerVIP -1 \
                                $ENROLLMENT_DIR \
                                $OUTPUT_DIR/enroll_list.csv
echo | ./create_speech_embeddings.py $OUTPUT_DIR/enroll_list.csv \
                                     $OUTPUT_DIR/speech-embeddings.pkl \
                                     $MAX_AUDIO_SEC
echo | ./create_speech_classifier.py $OUTPUT_DIR/speech-embeddings.pkl \
                                    $OUTPUT_DIR/svm.pkl \
                                    $OUTPUT_DIR/label
echo | ./calibrate_classifier_score.py $OUTPUT_DIR/svm.pkl \
                                      $OUTPUT_DIR/label \
                                      $OUTPUT_DIR/meta_vosk.pkl 
echo | ./skSvm2Java.py $OUTPUT_DIR/svm.pkl \
                      $OUTPUT_DIR/svm_vosk.json
./create_profile_zip.bash $ENROLLMENT_DIR $OUTPUT_DIR/profiles.zip

# save the atak data package.  USE the enrollment as UUID
UUID=${UUID_PREFIX}$(basename $ENROLLMENT_DIR)
ATAKZIPFILE="atak_uuid="${UUID}.zip
./create_atak_data_package.bash $OUTPUT_DIR $UUID $ATAKZIPFILE

# create cross validation result.
./run_perfEval.bash $ENROLLMENT_DIR $OUTPUT_DIR $OUTPUT_DIR/speakerid-3-fold-cross-validation.pdf

# set the timestamp of the atak zip file to the latest timestamp in the mission directory
# But, we don't know if any code is changed..
# latest=$(find -L $ENROLLMENT_DIR -maxdepth 5 -printf "%C@\t%p\n" | sort -n | tail -1 | cut -f 2)
# test -f $OUTPUT_DIR/$ATAKZIPFILE
# touch -r $latest $OUTPUT_DIR/$ATAKZIPFILE

# Cleanups
cd -
