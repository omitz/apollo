FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

# lib* for open-cv
RUN apt-get update && apt-get install -y python-tk libsm6 libxext6 libyaml-dev mediainfo && pip install --upgrade pip setuptools wheel

RUN mkdir code/
WORKDIR /code

COPY utils/ utils/
RUN pip install ./utils/

COPY apollo/ apollo/
RUN pip install ./apollo/

COPY obj_det_vid/requirements.txt .

RUN pip install -r requirements.txt

COPY obj_det_vid/. .

ENV PYTHONPATH "${PYTHONPATH}:/"
# Suppress tensorflow warnings
ENV TF_CPP_MIN_LOG_LEVEL "2"

CMD ["bash", "docker-entrypoint.sh"]