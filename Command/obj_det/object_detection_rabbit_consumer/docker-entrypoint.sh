#!/bin/bash
if [[ ${DEVELOPMENT^^} == "TRUE" ]]
then
    echo "starting watchmedo..."
    watchmedo auto-restart --directory=/usr/local/lib/python3.6/site-packages/apollo --directory=./obj_det --recursive --pattern=*.py -- python -u obj_det/object_detection_rabbit_consumer/main.py
else
    echo "starting python..."
    python -u obj_det/object_detection_rabbit_consumer/main.py
fi