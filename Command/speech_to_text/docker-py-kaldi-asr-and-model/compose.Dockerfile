FROM quay.io/mpuels/docker-py-kaldi-asr:0.4.1

## TC 2019-11-14 (Thu) -- added lsof
## TC 2019-11-20 (Wed) -- added update and gstreamer stuff
## TC 2020-03-18 (Wed) -- use ffmpeg instead of gstreamer
RUN echo "deb http://www.deb-multimedia.org jessie main non-free" >> /etc/apt/sources.list

RUN apt-get update && \
    apt-get install xz-utils -y && \
    apt-get install lsof -y && \
    apt-get install ffmpeg -y --force-yes && \
    apt-get autoremove -y --purge && \
    apt-get clean all


#ARG MODEL_NAME=kaldi-generic-en-tdnn_250-r20180815
## TC 2019-11-14 (Thu) -- Use the latest model
ARG MODEL_NAME=kaldi-generic-en-tdnn_250-r20190609

WORKDIR /opt
RUN wget -q http://goofy.zamia.org/zamia-speech/asr-models/${MODEL_NAME}.tar.xz
#COPY kaldi-generic-en-tdnn_250-r20190609.tar.xz /opt
RUN tar xf ${MODEL_NAME}.tar.xz
RUN mv ${MODEL_NAME} kaldi-model && rm ${MODEL_NAME}.tar.xz

EXPOSE 80

## TC 2019-11-14 (Thu) -- bash program that decodes the audio file
COPY ./speech_to_text/docker-py-kaldi-asr-and-model/decode_audio.bash /opt/asr_server
COPY ./speech_to_text/docker-py-kaldi-asr-and-model/asr_client.py /opt/asr_server
COPY ./speech_to_text/docker-py-kaldi-asr-and-model/demo3.wav /opt/asr_server

RUN pip install --upgrade pip
RUN pip install pika
RUN pip install pysocks
RUN pip install urllib3==1.23
RUN pip install boto3
RUN pip install enum
RUN pip install mpu

COPY /utils/commandutils /opt/asr_server/commandutils
#RUN pip install opt/asr_server/utils/

COPY ./speech_to_text/docker-py-kaldi-asr-and-model/rabbit_worker.py /opt/asr_server


WORKDIR /opt/asr_server
# CMD ["python", "rabbit_worker.py", "-o", "outfile"] TC 2019-11-22
# (Fri) -- By default we want to save back to S3, so we don't use -o
# option.
CMD ["python", "rabbit_worker.py"]
