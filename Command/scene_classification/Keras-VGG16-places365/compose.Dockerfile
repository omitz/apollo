FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/tensorflow/tensorflow:2.1.0-py3

# 1.) Set Directories:
ARG APP_DIR=/code
ARG APOLLO_DIR=scene_classification/Keras-VGG16-places365
WORKDIR $APP_DIR

# 2.) Install System Packages:
RUN apt-get clean all && apt-get update || true #  nvidia apt-get error..
RUN apt-get install wget libsm6 libxext6 libxrender-dev libpq-dev libgl1 -y
RUN python3 -m pip install --upgrade pip setuptools wheel

# 3.) Install Apollo specifics and utils:
COPY apollo/ /apollo/
RUN python3 -m pip install /apollo/

# 4.) Install Python Packages:
COPY $APOLLO_DIR/requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt

# 5.) Install App
## program and data files
COPY $APOLLO_DIR/vgg16_places_365.py $APP_DIR/
# Download neural network weights:
# Alternatively, we should download it (from S3) everytime the container starts.
#RUN python -c "from vgg16_places_365 import VGG16_Places365; VGG16_Places365(weights='places')" # download neural network weights
COPY $APOLLO_DIR/tests $APP_DIR/tests
COPY $APOLLO_DIR/6.jpg                          \
     $APOLLO_DIR/categories_places365.txt       \
     $APOLLO_DIR/scene_hierarchy.csv            \
     $APOLLO_DIR/vgg16Places365_main.py         \
     $APOLLO_DIR/main.py                        \
     $APOLLO_DIR/places365_rabbit_consumer.py   \
     $APOLLO_DIR/places365_analytic.py          \
     $APOLLO_DIR/main.py                        \
     $APOLLO_DIR/datasetIngest_v3.py            \
     $APOLLO_DIR/docker-entrypoint.sh $APP_DIR/

# 6.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all

# 7.) Start the App
CMD ["bash", "docker-entrypoint.sh"]

