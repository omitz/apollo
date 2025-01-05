FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.7-buster

# 1.) Set the app directory:
ARG APP_DIR=/code
ARG APOLLO_DIR=ocr/tesseract
WORKDIR $APP_DIR

# 2.) Install System Packages:
RUN apt-get update && \
    apt-get install tesseract-ocr tesseract-ocr-all libgif-dev -y
RUN python3 -m pip install --upgrade pip setuptools wheel

# 3.) Install Apollo specifics and utils:
COPY apollo/ /apollo/
RUN python3 -m pip install /apollo/

# 4.) Install Python Packages:
COPY $APOLLO_DIR/requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt

# 5.) Install App
## Download models
RUN mkdir -p /usr/share/tessdata
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/eng.traineddata && cd - # English
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/osd.traineddata && cd - # Orientation and script detection
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/ara.traineddata && cd - # Arabic
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/fra.traineddata && cd - # French
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/rus.traineddata && cd - # Russian
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/spa.traineddata && cd - # Spanish

ENV TESSDATA_PREFIX /usr/share/tessdata
# ENV TESSDATA_PREFIX /usr/share/tesseract-ocr/4.00/tessdata/

COPY $APOLLO_DIR/tests $APP_DIR/tests
COPY $APOLLO_DIR/tesseract_main.py              \
$APOLLO_DIR/ocr.py                              \
$APOLLO_DIR/main.py                             \
$APOLLO_DIR/tesseract_rabbit_consumer.py        \
$APOLLO_DIR/tesseract_analytic.py               \
$APOLLO_DIR/docker-entrypoint.sh $APP_DIR/

# 6.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all

# 7.) Start the App
CMD ["bash", "docker-entrypoint.sh"]
