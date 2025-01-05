# VOSK Speech Recognition

https://alphacephei.com/vosk/
https://alphacephei.com/vosk/models.html

### Runing Unittest
```bash
# from outside docker container:
python3 -m unittest

# from inside docker container:
export INSIDE_DOCKER=True
python3 -m unittest
## alternatively
docker-compose build --build-arg DOWNLOAD_MODEL_DURING_BUILD=1 speech-to-text
docker-compose run -e INSIDE_DOCKER=True --rm speech-to-text python3 -m unittest
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
```bash
# cd to Command dir.
docker-compose run -v $(pwd)/speech_to_text/vosk/:/code --rm speech-to-text bash
```

## Stand-alone version that can be run from host directly:
```bash
# if needed:
docker build -t runvosk .

# execute from host directly.
./runvosk.bash <audio_file>
```
