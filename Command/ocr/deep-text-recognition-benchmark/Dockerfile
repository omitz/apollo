FROM pytorch/pytorch:1.1.0-cuda10.0-cudnn7.5-runtime

## avoid need for cli input (tzdata asks for geographic location)
#ARG DEBIAN_FRONTEND=noninteractive

# lib* for open-cv
RUN apt-get -y update && apt-get install -y python-tk libsm6 libxext6 libglib2.0-0 && pip install --upgrade pip

COPY . .

RUN pip install -r requirements.txt
