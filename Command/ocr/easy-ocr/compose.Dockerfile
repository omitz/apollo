FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/pytorch/pytorch:1.6.0-cuda10.1-cudnn7-runtime

# 1.) Set the app directory:
ARG APP_DIR=/code
ARG APOLLO_DIR=ocr/easy-ocr
WORKDIR $APP_DIR

# 2.) Install System Packages:
RUN apt-get update && \
    apt-get install libsm6 libxext6 libxrender-dev libpq-dev libgl1 gcc -y

# 3.) Install Apollo specifics and utils:
COPY apollo/ /apollo/
RUN python3 -m pip install --upgrade pip setuptools wheel

RUN python3 -m pip install /apollo/

# 4.) Install Python Packages:
COPY $APOLLO_DIR/requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt



## fix opencv manually
RUN python3 -m pip uninstall opencv-python -y
RUN python3 -m pip install opencv-python-headless==4.4.0.42

# 5.) Install App
# pre-download some models
# Alternatively, we should download it (from S3) everytime the container starts.
# RUN python3 -c "import easyocr; easyocr.Reader(['ch_tra','en'])" # download some models
RUN python3 -c "import easyocr; easyocr.Reader(['ar','en'])" # download some models
COPY $APOLLO_DIR/tests $APP_DIR/tests
COPY $APOLLO_DIR/easyOcr_main.py               \
     $APOLLO_DIR/main.py                       \
     $APOLLO_DIR/easy_ocr_rabbit_consumer.py   \
     $APOLLO_DIR/easy_ocr_analytic.py          \
     $APOLLO_DIR/docker-entrypoint.sh $APP_DIR/

# 6.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all

# 7.) Start the App
CMD ["bash", "docker-entrypoint.sh"]
