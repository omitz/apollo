FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/tensorflow/tensorflow:2.1.0-gpu-py3

# 1.) Set the app directory:
ARG APP_DIR=/code
ARG APOLLO_DIR=ocr/keras-ocr-v2
WORKDIR $APP_DIR

# 2.) Install System Packages:
RUN apt-get update || true      #  nvidia apt-get error..
RUN apt-get install libsm6 libxext6 libxrender-dev libpq-dev libgl1 -y
RUN python3 -m pip install --upgrade pip setuptools wheel

# 3.) Install Apollo specifics and utils:
COPY apollo/ /apollo/
RUN python3 -m pip install /apollo/

# 4.) Install Python Packages:
COPY $APOLLO_DIR/requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt

# 5.) Install App
# COPY dot_keras-ocr /root/.keras-ocr    # about 100MB, download it instead
# Alternatively, we should download it (from S3) everytime the container starts.
#RUN python -c "import keras_ocr; keras_ocr.pipeline.Pipeline()" # download model
COPY $APOLLO_DIR/tests $APP_DIR/tests
COPY $APOLLO_DIR/kerasOcr_main.py               \
     $APOLLO_DIR/main.py                        \
     $APOLLO_DIR/keras_ocr_rabbit_consumer.py   \
     $APOLLO_DIR/keras_ocr_analytic.py          \
     $APOLLO_DIR/docker-entrypoint.sh $APP_DIR/

# 6.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all

# 7.) Start the App
CMD ["bash", "docker-entrypoint.sh"]
