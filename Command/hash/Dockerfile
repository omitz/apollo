FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

# lib* for open-cv
RUN apt-get update && apt-get install -y

RUN pip install --upgrade pip setuptools wheel

RUN mkdir code
WORKDIR /code

COPY hash/requirements.txt .
RUN pip install -r requirements.txt

COPY apollo/ apollo/
RUN pip install ./apollo/

COPY hash/ ./

CMD ["bash", "docker-entrypoint.sh"]
