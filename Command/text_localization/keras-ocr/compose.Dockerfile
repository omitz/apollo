FROM tensorflow/tensorflow:2.1.0-gpu-py3

# avoid need for cli input (tzdata asks for geographic location)
ARG DEBIAN_FRONTEND=noninteractive

RUN pip install --upgrade pip setuptools wheel
RUN pip install keras-ocr==0.8.3
RUN pip install ipython         # good for debugging

RUN apt-get update
RUN apt-get install libsm6 libxext6 libxrender-dev -y
RUN apt-get clean all

## Download models
# Alternatively, we should download it (from S3) everytime the container starts.
RUN python -c "import keras_ocr; keras_ocr.pipeline.Pipeline()" # download model

# Copy programs
RUN mkdir -p /app
WORKDIR /app
COPY text_localization/keras-ocr/main.py /app/
COPY text_localization/keras-ocr/ocr_keras_wrapper.py /app/
COPY text_localization/keras-ocr/Army_Reserves_Recruitment_Banner_MOD_45156284.jpg /app/

# Apollo specific:
COPY utils/ /utils/
RUN pip install /utils/

CMD ["python", "main.py"]
