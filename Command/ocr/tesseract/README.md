# tesseract OCR

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
docker-compose run -v $(pwd)/ocr/tesseract/:/code --rm ocr-tesseract bash
```

