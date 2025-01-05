FROM ubuntu:18.04

RUN apt-get update && apt-get install \
    -y --no-install-recommends python3 python3-virtualenv

ENV VIRTUAL_ENV=/opt/venv
RUN python3 -m virtualenv --python=/usr/bin/python3 $VIRTUAL_ENV
ENV PATH="$VIRTUAL_ENV/bin:$PATH"
RUN pip install --upgrade pip setuptools wheel

COPY requirements.txt /app/
RUN pip install -r /app/requirements.txt

COPY in.txt /app/
COPY textblob_wrapper.py /app/

WORKDIR /app

CMD ["./textblob_wrapper.py", "in.txt", "out.txt"]
