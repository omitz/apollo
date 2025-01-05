# Running Apollo Docker Image

Assuming you already built the docker image, you can test it by:

`docker run --rm -it <docker_image> ./decode_audio.bash ./demo3.wav`

You should get:

`
philip knew that she was not an indian
`

*Note: This docker image can take any audio format, not just wav.*

To use S3 bucket, you need to specify the AWS credential and secret key do:

`docker run --rm -it <docker_image> ./rabbit_worker.py --help`


To increase docker /dev/shm to 512mb do:

`docker run --rm --shm-size=512m -it <docker_image> ./decode_audio.bash ./demo3.wav`



# STT Service based on Kaldi ASR

This Docker image contains a demo STT service based on [Kaldi
ASR](https://github.com/kaldi-asr/kaldi) and
[py-kaldi-asr](https://github.com/gooofy/py-kaldi-asr). Try it out by
following these steps.

## Install the Docker Image
We are going to install a docker image and name it
"apollo/py-kaldi-asr".  The resulting docker container will run a
Speech-To-Text (STT) service and decode any wav file sent to localhost
port 8080 (or whatever port you want).

### Method1: Get the Pre-built Docker Image from the Quay.io Repository
This method is the most direct way to install the docker image.

#### 1.) Install the Original Docker image:
```bash
docker pull quay.io/mpuels/docker-py-kaldi-asr-and-model:kaldi-generic-en-tdnn_sp-r20180815 
```
#### 2.) Rename the Image to "apollo/py-kaldi-asr":
```bash
docker tag quay.io/mpuels/docker-py-kaldi-asr-and-model:kaldi-generic-en-tdnn_sp-r20180815 apollo/py-kaldi-asr
```
#### 3.) Delete the Original Image:
```bash
docker rmi quay.io/mpuels/docker-py-kaldi-asr-and-model:kaldi-generic-en-tdnn_sp-r20180815 
```

### Method2: Build the Docker Image from Dockerfile


This method builds the docker image by using another pre-built image
(quay.io/mpuels/docker-py-kaldi-asr:0.4.1).
```bash
docker build -t apollo/py-kaldi-asr .
```
*Note*: If build fails, you may need to update the model url in the Dockerfile.


## Save the Docker Image as a Stand-Alone Docker Image

Now that we have installed the docker image.  We want to save the
image back as a stand-alone image file.  This way, when we later
install from this saved image file, docker will not need to pull any
dependent images from repositories such as Docker Hub or quay.io.

```bash
docker save apollo/py-kaldi-asr | pigz > docker_apollo_py-kaldi-asr.tgz
```

## Run/Start the Docker Container

### Start the TTS service
```bash
docker run --rm -p 8080:80/tcp apollo/py-kaldi-asr
```

### Transfer an Audio File for Transcription

#### Setup Python Enviromenet
```bash
virtualenv py-kaldi-asr-env
source py-kaldi-asr-env/bin/activate
```
##### Install needed Python Packages
```bash
(py-kaldi-asr-env) $: (
  pip install requests
)
```

#### Run

```bash
(py-kaldi-asr-env) $: (
./asr_client.py asr.wav
)
```

### More Info
For a list of available Kaldi models packaged in Docker containers, see
https://quay.io/repository/mpuels/docker-py-kaldi-asr-and-model?tab=tags

For a description of the available models, see
https://github.com/gooofy/zamia-speech#asr-models .

Docker images are named according to the format

    kaldi-generic-<LANG>-tdnn-<SIZE>-<RELEASEDATE>

1. `<LANG>`: There are models for English (`en`) and German (`de`).
2. `<SIZE>`: Kaldi models come in two sizes: `sp` (standard size) and `250` (
   smaller size, suitable for realtime decoding on Raspberry Pi).
3. `<RELEASEDATE>`: Usually, models released later are trained on more data and
   hence have a lower word error rate.

The image is part of [Zamia Speech](https://github.com/gooofy/zamia-speech).

## Current Status and Random Thoughts:
 - Need to create input and ouput rabbitQ interface. (intead of using
   port 8080).
