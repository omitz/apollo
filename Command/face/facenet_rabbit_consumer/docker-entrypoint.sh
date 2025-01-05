#!/bin/bash
if [[ ${DEVELOPMENT^^} == "TRUE" ]]
then
    echo "starting watchmedo..."
    watchmedo auto-restart --directory=/usr/local/lib/python3.6/site-packages/apollo --directory=./face/facenet_rabbit_consumer --recursive --pattern=*.py -- python -u face/facenet_rabbit_consumer/main.py
else
    echo "starting python..."
    python -u face/facenet_rabbit_consumer/main.py
fi