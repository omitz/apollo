#!/bin/bash
if [[ ${DEVELOPMENT^^} == "TRUE" ]]
then
    echo "starting watchmedo..."
    watchmedo auto-restart --directory=./ --recursive --pattern=*.py -- python main.py
else
    echo "starting python..."
    python main.py
fi