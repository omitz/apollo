# EasyOCR

Ready-to-use OCR with 70+ languages supported including Chinese, Japanese, Korean and Thai.

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
docker-compose run -v $(pwd)/ocr/easy-ocr/:/code --rm ocr-easy bash
```
