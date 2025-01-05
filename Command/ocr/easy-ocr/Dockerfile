FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/pytorch/pytorch:1.6.0-cuda10.1-cudnn7-runtime


# 1.) Set the app directory:
ARG APP_DIR=/app
WORKDIR $APP_DIR

# 2.) Install System Packages:
RUN apt-get update && apt-get install libsm6 libxext6 libxrender-dev libgl1 -y

# 3.) Install Python Packages:
RUN python3 -m pip install --upgrade pip setuptools wheel
COPY requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt
## fix opencv manually
RUN python3 -m pip uninstall opencv-python -y
RUN python3 -m pip install opencv-python-headless==4.4.0.42


# 4.) Install App
# pre-download some models
# RUN python3 -c "import easyocr; easyocr.Reader(['ch_tra','en'])" # download some models
RUN python3 -c "import easyocr; easyocr.Reader(['ar','en'])" # download some models

COPY easyOcr_main.py $APP_DIR/
COPY tests $APP_DIR/tests


# 5.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all


# 6.) Start the App
# CMD ["./easyOcr_main.py", "tests/images/chinese.jpg", "/dev/stdout", "out.png", "debug.json"]
# run -i easyOcr_main.py "tests/images/Army_Reserves_Recruitment_Banner_MOD_45156284.jpg" "/dev/stdout" "out.png" "debug.json"
# run -i easyOcr_main.py "tests/images/chinese.jpg" "/dev/stdout" "out.png" "debug.json"
CMD ["./easyOcr_main.py", "tests/images/chinese.jpg", "/dev/stdout", "out.png", "debug.json"]
