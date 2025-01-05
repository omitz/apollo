FROM tensorflow/tensorflow:1.14.0-gpu-py3

# avoid need for cli input (tzdata asks for geographic location)
ARG DEBIAN_FRONTEND=noninteractive

# lib*6 for open-cv
RUN apt-get -y update && apt-get install -y python-tk libsm6 libxext6 libasound-dev portaudio19-dev libportaudiocpp0 libsndfile1 && pip install --upgrade pip

# avoid numba permissions error https://github.com/ska-sa/montblanc/issues/253
ENV NUMBA_CACHE_DIR=/tmp/numba_cache

COPY . .

RUN pip install -r requirements.txt
