#This dockerfile is intending for running unit tests, this module does nothing on its own

FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

# lib* for open-cv
RUN apt-get update && apt-get install -y python-tk libsm6 libxext6 libyaml-dev
RUN pip install --upgrade pip setuptools wheel

ENV PYTHONBUFFERED 1

RUN mkdir code
WORKDIR /code

COPY apollo/ ./
RUN pip install ./

CMD ["watchmedo", "auto-restart", "--directory=./tests", "--directory=/usr/local/lib/python3.6/site-packages/apollo", "--recursive", "--pattern=*.py", "--", "python", "-mu", "unittest"]
