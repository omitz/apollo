#!/bin/bash
if [[ ${DEVELOPMENT^^} == "TRUE" ]]
then
    echo "starting watchmedo..."
    watchmedo auto-restart --directory=./ --recursive --pattern=*.py -- python ner_spacy/main.py
else
    echo "starting python..."
    python ner_spacy/main.py
fi