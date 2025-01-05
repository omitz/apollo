FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

# avoid need for cli input
ARG DEBIAN_FRONTEND=noninteractive

# lib* for open-cv
RUN apt-get update  && apt-get install -y python-tk libsm6 libxext6 libyaml-dev && pip install --upgrade pip

RUN mkdir code

COPY obj_det/object_detection_rabbit_consumer/requirements.txt ./code

RUN pip install --upgrade pip setuptools wheel
RUN pip install -r ./code/requirements.txt

COPY apollo/ code/apollo/
RUN pip install ./code/apollo/

#open source code
COPY obj_det/ ./code/obj_det/

ENV PYTHONPATH "${PYTHONPATH}:code/obj_det"
ENV PYTHONBUFFERED "1"

WORKDIR "/code/"
CMD ["bash", "obj_det/object_detection_rabbit_consumer/docker-entrypoint.sh"]