#!/bin/bash
if [[ ${DEVELOPMENT^^} == "TRUE" ]]
then
    echo "starting watchmedo..."
    watchmedo auto-restart --directory=/usr/local/lib/python3.6/dist-packages/apollo --directory=./ --recursive --pattern=*.py -- python speaker_recognition/vgg_speaker_recognition/main.py
else
    echo "starting python..."
    python speaker_recognition/vgg_speaker_recognition/main.py
fi