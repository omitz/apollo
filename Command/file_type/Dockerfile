FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/ubuntu:latest

RUN apt-get update && apt-get install \
    -y --no-install-recommends python3 python3-virtualenv
RUN apt-get update
RUN apt-get install -y python3-magic

ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m virtualenv --python=/usr/bin/python3 $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"

RUN pip install --upgrade pip setuptools wheel

COPY file_type/requirements.txt .

RUN pip install -r requirements.txt
COPY apollo/ apollo/
RUN pip install ./apollo

COPY file_type/ .

CMD ["python", "file_checker_rb.py", "--useS3", "True", "--rxOn", "True"]
