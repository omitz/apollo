FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.6.9

RUN apt-get update && apt-get install -y python-tk libsm6 libxext6 python-dev libxml2-dev libxslt1-dev antiword unrtf poppler-utils libjpeg-dev swig libpulse-dev libyaml-dev && pip install --upgrade pip setuptools wheel

RUN mkdir code
WORKDIR code/

COPY ner/ ./

RUN pip install -r ner_spacy/requirements.txt
COPY utils/ utils/
RUN pip install ./utils
RUN python -m spacy download en_core_web_sm		
RUN python -m spacy download fr_core_news_sm		
RUN python -m spacy download xx_ent_wiki_sm

ENV PYTHONPATH "${PYTHONPATH}:/"

CMD ["bash", "docker-entrypoint.sh"]