FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

# lib* for open-cv
RUN apt-get update  && apt-get install -y python-tk libsm6 libsndfile1 libxext6 libyaml-dev ffmpeg && pip install --upgrade pip

RUN mkdir code

COPY speaker_recognition/speaker_search/requirements.txt ./code

RUN pip install --upgrade pip setuptools wheel
RUN pip install -r ./code/requirements.txt

COPY apollo/ code/apollo/
RUN pip install ./code/apollo/

RUN mkdir code/speaker_recognition

# Allow unittest to find tests
COPY speaker_recognition/__init__.py ./code/speaker_recognition

COPY speaker_recognition/vgg_speaker_recognition ./code/speaker_recognition/vgg_speaker_recognition
COPY speaker_recognition/speaker_search ./code/speaker_recognition/speaker_search

ENV PYTHONPATH "${PYTHONPATH}:code/"
ENV PYTHONBUFFERED "1"

WORKDIR "/code/"
CMD ["python", "-u", "speaker_recognition/speaker_search/main.py"]