FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

# lib* for open-cv
RUN apt-get update && apt-get install -y python-tk libsm6 libxext6 && pip install --no-cache-dir --upgrade pip setuptools wheel

COPY landmark_recognition/requirements.txt /code/requirements.txt

WORKDIR /code

RUN pip install --no-cache-dir -r requirements.txt

COPY apollo/ apollo/
RUN pip install ./apollo/

COPY ./landmark_recognition/ /code/

ENV PYTHONPATH "${PYTHONPATH}:/code"

CMD ["python", "search/wsgi.py"]