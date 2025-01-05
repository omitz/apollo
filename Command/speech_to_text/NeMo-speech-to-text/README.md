# NeMo ASR Application

## Create Docker Image:
```
docker build -t apollo/nemo-asr .
```

## Run Docker Container:
```
docker run -it --rm -v $(pwd):/host --shm-size=16g --ulimit memlock=-1 --ulimit stack=67108864 apollo/nemo-asr ./run-dir.py audio-dir/ /host/out.txt

cat ./out.txt
```
