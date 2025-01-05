FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/tensorflow/tensorflow:2.1.0-gpu-py3

# 1.) Set the app directory:
ARG APP_DIR=/app
WORKDIR $APP_DIR


# 2.) Install System Packages:
# avoid need for cli input (tzdata asks for geographic location)
ARG DEBIAN_FRONTEND=noninteractive 
RUN apt-get update || true      #  nvidia apt-get error..
RUN apt-get install libsm6 libxext6 libxrender-dev libgl1 -y


# 3.) Install Python Packages:
RUN python3 -m pip install --upgrade pip setuptools wheel
COPY requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt


# 4.) Install App
# COPY dot_keras-ocr /root/.keras-ocr    # about 100MB, download it instead
# Alternatively, we should download it (from S3) everytime the container starts.
RUN python3 -c "import keras_ocr; keras_ocr.pipeline.Pipeline()" # download model
COPY kerasOcr_main.py $APP_DIR/
COPY tests $APP_DIR/tests


# 5.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all


# 6.) Start the App
CMD ["./kerasOcr_main.py", "tests/images/Army_Reserves_Recruitment_Banner_MOD_45156284.jpg", "/dev/stdout", "out.png", "debug.txt"]
