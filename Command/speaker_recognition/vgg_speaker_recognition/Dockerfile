FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

# lib*6 for open-cv, libsndfile1 for soundfile, libpq-dev for psycopg2, ffmpeg for librosa backend
RUN apt-get update || true      #  nvidia apt-get error..
RUN apt-get install -y python-tk libsm6 libxext6 libsndfile1 libpq-dev ffmpeg && pip install --upgrade pip setuptools wheel

# avoid numba permissions error https://github.com/ska-sa/montblanc/issues/253
ENV NUMBA_CACHE_DIR=/tmp/numba_cache

ENV TF_CPP_MIN_LOG_LEVEL=2

RUN mkdir code
RUN mkdir code/speaker_recognition

COPY speaker_recognition/__init__.py ./code/speaker_recognition

COPY speaker_recognition/vgg_speaker_recognition/requirements.txt ./code
RUN pip install -r ./code/requirements.txt --no-cache-dir

COPY apollo/ apollo/
RUN pip3 install ./apollo/

COPY speaker_recognition/vgg_speaker_recognition/ ./code/speaker_recognition/vgg_speaker_recognition

ENV PYTHONPATH "${PYTHONPATH}:code/"

WORKDIR "/code/"

CMD ["bash", "speaker_recognition/vgg_speaker_recognition/docker-entrypoint.sh"]