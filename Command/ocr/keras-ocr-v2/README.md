# Keras-OCR

Great OCR pipeline using Craft for text localization but with OCR
decoding as well. It only works on English though.

Installable as a python package (keras-ocr).

### Runing Unittest 
```bash
# from outside docker container:
python3 -m unittest

# from inside docker container:
export JENKINS=True
python3 -m unittest
```

## Apollo Integration:
Assumptions:
    - Docker-Compose is installed.
    - The Apollo Command directory located at ../../..
    - There is a .env file in the Apollo Command directory.
### Runing Unittest
```bash
./tests/integration_test.sh
```
### Live debugging
```
# cd to Command dir.
docker-compose run -v $(pwd)/ocr/keras-ocr-v2/:/code --rm ocr-keras bash
#or
docker-compose exec ocr-keras bash
```
