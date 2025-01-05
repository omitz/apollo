FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.7-buster

# 0.) Build Environment 
# set it to anything to download model during build --
# needed for local integration test.
ARG DOWNLOAD_MODEL_DURING_BUILD

# 1.) Set Directories:
ARG APP_DIR=/code
ARG APOLLO_DIR=speech_to_text/vosk
WORKDIR $APP_DIR

# 2.) Install System Packages:
RUN apt-get update && \
    apt-get install xz-utils -y && \
    apt-get install lsof -y && \
    apt-get install ffmpeg -y --force-yes
RUN python3 -m pip install --upgrade pip setuptools wheel

# 3.) Install Apollo specifics and utils:
COPY apollo/ /apollo/
RUN python3 -m pip install /apollo/

# 4.) Install Python Packages:
COPY $APOLLO_DIR/requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt

# 5.) Install App
## Model: Accurate wideband model for dictation from Kaldi-active-grammar
## project with configurable graph:
#ARG MODEL_NAME=vosk-model-en-us-daanzu-20200328-lgraph
#ADD https://github.com/daanzu/kaldi-active-grammar/releases/download/v1.4.0/${MODEL_NAME}.zip $APP_DIR/

## Model: Trained on Fisher + more or less recent LM. Should be pretty good for
## generic US English transcription
#ARG MODEL_NAME=vosk-model-en-us-aspire-0.2
#ADD https://alphacephei.com/vosk/models/${MODEL_NAME}.zip $APP_DIR/

#RUN unzip ${MODEL_NAME}.zip 
#RUN ln -s ${MODEL_NAME} model

RUN if [ "$DOWNLOAD_MODEL_DURING_BUILD" == 1] ; then            \
    MODEL_NAME=vosk-model-en-us-aspire-0.2;                     \
    cd $APP_DIR/;                                               \
    wget https://alphacephei.com/vosk/models/${MODEL_NAME}.zip; \
    unzip ${MODEL_NAME}.zip;                                    \
    ln -s ${MODEL_NAME} model;                                  \
    rm ${MODEL_NAME}.zip;                                       \
    fi

COPY $APOLLO_DIR/tests $APP_DIR/tests
COPY $APOLLO_DIR/vosk_main.py                   \
     $APOLLO_DIR/main.py                        \
     $APOLLO_DIR/docker-entrypoint.sh           \
     $APOLLO_DIR/vosk_rabbit_consumer.py        \
     $APOLLO_DIR/vosk_analytic.py $APP_DIR/

# 6.) clean-ups
#RUN rm ${MODEL_NAME}.zip
RUN apt-get autoremove -y --purge && apt-get clean all

# 7.) Start the App
CMD ["bash", "docker-entrypoint.sh"]
