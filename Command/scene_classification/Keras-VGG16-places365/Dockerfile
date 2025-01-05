FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/tensorflow/tensorflow:2.1.0-gpu-py3

# 1.) Set the app directory:
ARG APP_DIR=/app
WORKDIR $APP_DIR


# 2.) Install System Packages:
RUN apt-get clean all && apt-get update || true #  nvidia apt-get error..
RUN apt-get install wget libsm6 libxext6 libxrender-dev libgl1 -y


# 3.) Install Python Packages:
RUN python3 -m pip install --upgrade pip setuptools wheel
COPY requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt


# 4.) Install App
## program and data files
COPY vgg16_places_365.py $APP_DIR/
# Download neural network weights:
# Alternatively, we should download it (from S3) everytime the container starts.
RUN python -c "from vgg16_places_365 import VGG16_Places365; VGG16_Places365(weights='places')" # download neural network weights
COPY 6.jpg $APP_DIR/
COPY categories_places365.txt $APP_DIR/
COPY scene_hierarchy.csv $APP_DIR/
COPY vgg16Places365_main.py $APP_DIR/
COPY tests $APP_DIR/tests


# 5.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all

# 6.) Start the App
CMD ["./vgg16Places365_main.py", "6.jpg", "/dev/stdout"]
