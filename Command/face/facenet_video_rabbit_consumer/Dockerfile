FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

# avoid need for cli input
ARG DEBIAN_FRONTEND=noninteractive

# lib* for open-cv
RUN apt-get update  && apt-get install -y python-tk libsm6 libxext6 libyaml-dev && pip install --upgrade pip

RUN mkdir code

COPY face/facenet_video_rabbit_consumer/requirements.txt ./code

RUN pip install --upgrade pip setuptools wheel
RUN pip install -r ./code/requirements.txt

COPY apollo/ code/apollo/
RUN pip install ./code/apollo/

RUN mkdir code/face

# Allow unittest to find tests
COPY face/__init__.py ./code/face

COPY face/facenet ./code/face/facenet

# Rm tests for the mtcnn implementation in facenet
RUN rm -r /code/face/facenet/src/align/tests

COPY face/facenet_video_rabbit_consumer ./code/face/facenet_video_rabbit_consumer

ENV PYTHONPATH "${PYTHONPATH}:code/face"
ENV PYTHONBUFFERED "1"

WORKDIR "/code/"
CMD ["bash", "face/facenet_video_rabbit_consumer/docker-entrypoint.sh"]
