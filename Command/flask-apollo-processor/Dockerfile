FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.8

RUN pip3 install --upgrade pip setuptools wheel

COPY ./flask-apollo-processor/requirements.txt /code/requirements.txt

WORKDIR /code

RUN pip3 install -r requirements.txt

COPY utils/ utils/
RUN pip3 install ./utils/

COPY apollo/ apollo/
RUN pip3 install ./apollo/

ADD ./flask-apollo-processor /code

CMD [ "python3", "wsgi.py" ]
