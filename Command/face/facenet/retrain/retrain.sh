#!/usr/bin/env bash
# From apollo/Command/face/facenet/retrain
export PYTHONPATH=$PYTHONPATH:../src

# Detect and crop the face out of each image
python3 align_dataset_mtcnn.py
# Train the classifier and save the new label mapping
python3 training_classifier.py

