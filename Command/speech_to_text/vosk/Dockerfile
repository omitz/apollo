FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.7-buster


# 1.) Set the app directory:
ARG APP_DIR=/app
WORKDIR $APP_DIR


# 2.) Install System Packages:
RUN apt-get update && \
    apt-get install xz-utils -y && \
    apt-get install lsof -y && \
    apt-get install ffmpeg -y --force-yes


# 3.) Install Python Packages:
RUN python3 -m pip install --upgrade pip setuptools wheel
COPY requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt


# 4.) Install App
## Model: Accurate wideband model for dictation from Kaldi-active-grammar
## project with configurable graph:
#ARG MODEL_NAME=vosk-model-en-us-daanzu-20200328-lgraph
#ADD https://github.com/daanzu/kaldi-active-grammar/releases/download/v1.4.0/${MODEL_NAME}.zip $APP_DIR/

## Model: Trained on Fisher + more or less recent LM. Should be pretty good for
## generic US English transcription
ARG MODEL_NAME=vosk-model-en-us-aspire-0.2
ADD https://alphacephei.com/vosk/models/${MODEL_NAME}.zip $APP_DIR/

RUN unzip ${MODEL_NAME}.zip
RUN ln -s ${MODEL_NAME} model
COPY vosk_main.py $APP_DIR/
COPY tests $APP_DIR/tests


# 5.) clean-ups
RUN rm ${MODEL_NAME}.zip
RUN apt-get autoremove -y --purge && apt-get clean all


# 6.) Start the App
CMD ["./vosk_main.py", "tests/audios/indian.mp3", "/dev/stdout"]
