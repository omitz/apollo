#!/bin/bash
#
# Train and create classifier model files for FacedID
#
# Also create an ATAK data package.  By convention, the name of UUID
# is "<uuid_prfix><enrollmenet>" and the zip file is "atak_uuid=<UUID>.zip"
#
# We assume that <enrollmenet> is the last directory in <enrollmenet_dir>
#
# TC 2021-09-28 (Tue) -- also create $OUTPUT_DIR/faceid-3-fold-cross-validation.pdf
#
set -ue

ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)

if ! [ $# -eq 3 ]; then
    cat <<EOF 
Usage:
  $0 <enrollment_dir> <output_dir> <uuid_prefix>

Example:
  $0 missions/celebrity10/ classifier_models/celebrity10/ FaceID_

EOF
    exit -1;
fi
ENROLLMENT_DIR=$1
OUTPUT_DIR=$2
UUID_PREFIX=$3

# Change to the directory where the excutables reside
cd $EXEDIR
mkdir -p $OUTPUT_DIR

# Train the FaceId classifier from scratch:
echo | ./create_face_dataset.py $ENROLLMENT_DIR \
                               $OUTPUT_DIR/faces-dataset.pkl
echo | ./create_face_embeddings_v2.py $OUTPUT_DIR/faces-dataset.pkl \
                                     $OUTPUT_DIR/faces-embeddings.pkl \
                                     20180402-114759/20180402-114759.pb
echo | ./create_face_classifier.py $OUTPUT_DIR/faces-embeddings.pkl \
                                  $OUTPUT_DIR/svm.pkl \
                                  $OUTPUT_DIR/label
echo | ./skSvm2LibSvm.py -s 0 \
                        $OUTPUT_DIR/svm.pkl \
                        $OUTPUT_DIR/model 
#echo | ./skSvm2LibSvm.py -s 1 svm.pkl model.desktop   # for desktop
./create_profile_zip.bash $ENROLLMENT_DIR $OUTPUT_DIR/profiles.zip

# save the atak data package.  USE the enrollment as UUID
UUID=${UUID_PREFIX}$(basename $ENROLLMENT_DIR)
ATAKZIPFILE="atak_uuid="${UUID}.zip
./create_atak_data_package.bash $OUTPUT_DIR $UUID $ATAKZIPFILE

# create cross validation result.
./run_perfEval.bash $ENROLLMENT_DIR $OUTPUT_DIR $OUTPUT_DIR/faceid-3-fold-cross-validation.pdf

# set the timestamp of the atak zip file to the latest timestamp in the mission directory
# latest=$(find -L $ENROLLMENT_DIR -maxdepth 5 -printf "%C@\t%p\n" | sort -n | tail -1 | cut -f 2)
# test -f $OUTPUT_DIR/$ATAKZIPFILE
# touch -r $latest $OUTPUT_DIR/$ATAKZIPFILE


# optional testing
#echo | ./predict_face_v2.py jim_gaffigan.jpg model.desktop label

# Cleanups
cd -
