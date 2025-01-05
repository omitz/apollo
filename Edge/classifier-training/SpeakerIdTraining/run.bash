#!/bin/bash

set -e
maxExamples=$1
if [[ "$maxExamples" == "" ]]; then
    maxExamples=30
fi

(echo | rm profiles.zip meta_vosk.pkl svm_vosk.json *.csv)  || true
echo | ./create_dataset_lists.py \
                     --nTrainPerVIP $maxExamples \
                     --nTestPerVIP -1 \
                     vips_with_profile/ \
                     enroll_list.csv \
                     test_list.csv
echo | ./create_speech_embeddings.py enroll_list.csv speech-embeddings.pkl
echo | ./create_speech_classifier.py speech-embeddings.pkl svm.pkl label
echo | ./calibrate_classifier_score.py svm.pkl label meta_vosk.pkl 
echo | ./skSvm2Java.py svm.pkl svm_vosk.json
./create_profile_zip.bash vips_with_profile/
./create_atak_data_package.bash speakerID_celebrity10
./create_atak_data_package.bash ApolloSpeakerID-10VIP # for Apollo Edge
