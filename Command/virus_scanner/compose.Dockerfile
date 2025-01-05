FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.8.1-buster

RUN apt-get update
RUN apt-get install -y clamav clamav-daemon

RUN mkdir code

WORKDIR code

#set up clam
RUN sed -i '1i TCPAddr 127.0.0.1' /etc/clamav/clamd.conf
RUN sed -i '1i TCPSocket 3310' /etc/clamav/clamd.conf
RUN mkdir /etc/systemd/system/clamav-daemon.socket.d/
ADD virus_scanner/extend.conf /etc/systemd/system/clamav-daemon.socket.d/extend.conf
RUN /etc/init.d/clamav-freshclam no-daemon # initial virus database

RUN pip install --upgrade pip setuptools wheel
COPY virus_scanner/requirements.txt .
RUN pip install -r requirements.txt

COPY apollo/ /apollo/
RUN pip install /apollo/

COPY virus_scanner/ .
CMD ./docker-entrypoint.sh
