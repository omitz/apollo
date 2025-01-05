FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.7-buster

# 1.) Set the app directory:
ARG APP_DIR=/app
WORKDIR $APP_DIR


# 2.) Install System Packages:
RUN apt-get update && \
    apt-get install tesseract-ocr libgif-dev -y


# 3.) Install Python Packages:
RUN python3 -m pip install --upgrade pip setuptools wheel
COPY requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt


# 4.) Install App
## Download models
RUN mkdir -p /usr/share/tessdata
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/eng.traineddata && cd - # English
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/osd.traineddata && cd - # Orientation and script detection
RUN cd /usr/share/tessdata && curl -JLO https://github.com/tesseract-ocr/tessdata_best/raw/master/ara.traineddata && cd - # Arabic

COPY tesseract_main.py $APP_DIR/
COPY ocr.py $APP_DIR/
COPY tests $APP_DIR/tests


# 5.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all


# 6.) Start the App
CMD ["./tesseract_main.py", "tests/images/ocr.png", "/dev/stdout", "out.png", "debug.txt"]

