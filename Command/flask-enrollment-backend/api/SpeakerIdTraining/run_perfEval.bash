#!/bin/bash

set -eu

if [[ ! $# -eq 3 ]]; then
    cat <<EOF 
Usage:
  $0 <missionDir> <foldDir> <outPdf>

Example:
  $0 missions/celebrity10/ /tmp/ /tmp/out.pdf
EOF
    exit 1;
fi

ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)

MISSION_DIR=$1
FOLD_DIR=$2
PDF_OUT_FILE=$3

#####################
## Exp1:
#####################
# do k-fold cross validation  (each class has 30 examples)
#echo | ./create_cross_validatation_data.py vips_with_profile_picture/ 3 
echo | ./create_cross_validatation_data.py $MISSION_DIR 3 $FOLD_DIR
PDF_CONFUSION_FILE=${PDF_OUT_FILE//.pdf/.confusion.pdf}
echo | ./show_cross_validation_result.py 3 $FOLD_DIR 50 ${PDF_CONFUSION_FILE} 

# show threshold studies
PDF_PLOTS_FILE=${PDF_OUT_FILE//.pdf/.plots.pdf}
echo | ./show_cv_threshold_study.py 3 $FOLD_DIR ${PDF_PLOTS_FILE} 

# combine pdfs
./combinePdfs.py ${PDF_CONFUSION_FILE} ${PDF_PLOTS_FILE} ${PDF_OUT_FILE}
#evince speakerid_threshold_study.pdf


