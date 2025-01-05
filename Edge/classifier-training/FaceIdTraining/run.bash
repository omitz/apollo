#!/bin/bash

set -e
maxExamples=$1

echo | ./create_face_dataset.py vips_with_profile/ faces-dataset.pkl $maxExamples
echo | ./create_face_embeddings_v2.py faces-dataset.pkl faces-embeddings.pkl
echo | ./create_face_classifier.py faces-embeddings.pkl svm.pkl label
echo | ./skSvm2LibSvm.py -s 0 svm.pkl model 
echo | ./skSvm2LibSvm.py -s 1 svm.pkl model.desktop   # for desktop
./create_profile_zip.bash vips_with_profile/
./create_atak_data_package.bash faceID_celebrity10
./create_atak_data_package.bash ApolloFaceID-10VIP # for apollo Edge


# optional testing
echo | ./predict_face_v2.py jim_gaffigan.jpg model.desktop label

echo "You should see > 0.7 detection for Jim Gaffigan"
echo "like Prob is  0.8805665543004734"

