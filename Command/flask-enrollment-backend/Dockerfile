FROM python:3.7.1

RUN pip3 install --upgrade pip setuptools wheel

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update -y
RUN apt install libgl1-mesa-glx -y
RUN apt-get install 'ffmpeg'\
    'libsm6'\
    'libxext6'\
    'portaudio19-dev'\
    'zip' -y

WORKDIR /code

COPY utils/ utils/
RUN pip3 install ./utils/

COPY apollo/ apollo/
RUN pip3 install ./apollo/

# Do last to overwrite any old apolo or utils python packages
COPY ./flask-enrollment-backend/requirements.txt /code/requirements.txt
RUN pip3 install -r requirements.txt

# Add enrollment files to what's already in /code/
ADD ./flask-enrollment-backend /code

CMD [ "python3", "wsgi.py" ]
