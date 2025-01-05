#!/usr/bin/env bash
# From Command/speaker_recognition/vgg_speaker_recognition/retrain
COMMAND_DIR=../../../../Command
export PYTHONPATH=$PYTHONPATH:$COMMAND_DIR

pip install --upgrade pip
pip install -r ../requirements.txt
python3 enroll.py