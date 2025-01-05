#!/bin/bash
if [[ ${DEVELOPMENT^^} == "TRUE" ]]
then
    echo "starting watchmedo..."
    watchmedo auto-restart --directory=./ --recursive --pattern=*.py -- ./main.py
else
    echo "starting python..."
    ./main.py
fi