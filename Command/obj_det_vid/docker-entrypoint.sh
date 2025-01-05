#!/bin/sh
if [[ ${DEVELOPMENT^^} == "TRUE" ]]
then
    echo "starting watchmedo..."
    watchmedo auto-restart --directory=./ --directory=/usr/local/lib/python3.6/site-packages/apollo --recursive --pattern=*.py -- python main.py
else
    echo "starting python..."
    python main.py
fi