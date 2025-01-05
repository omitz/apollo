#This dockerfile is intending for running unit tests, this module does nothing on its own

FROM python:3.6.8

# lib* for open-cv
RUN apt-get update && apt-get install -y python-tk libsm6 libxext6 libyaml-dev
RUN pip install --upgrade pip setuptools wheel

RUN mkdir code
WORKDIR /code

COPY utils/tests/requirements.txt ./tests/requirements.txt
RUN pip install -r ./tests/requirements.txt

COPY utils/ ./
RUN pip install ./

CMD ["watchmedo", "auto-restart", "--directory=./", "--recursive", "--pattern=*.py", "--", "python", "-m", "unittest"]
