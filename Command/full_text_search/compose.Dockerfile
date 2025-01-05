FROM 604877064041.dkr.ecr.us-east-1.amazonaws.com/python:3.7-buster

# 1.) Set Directories:
ARG APP_DIR=/code
ARG APOLLO_DIR=full_text_search
WORKDIR $APP_DIR

# 2.) Install System Packages:
RUN apt-get update && apt-get install antiword # Dependency for reading .doc
RUN python3 -m pip install --upgrade pip setuptools wheel

# 3.) Install Apollo specifics and utils:
COPY apollo/ /apollo/
RUN python3 -m pip install /apollo/

# 4.) Install Python Packages:
COPY $APOLLO_DIR/requirements.txt $APP_DIR/
RUN python3 -m pip install -r requirements.txt

# 5.) Install App
COPY $APOLLO_DIR/tests $APP_DIR/tests
COPY $APOLLO_DIR/main.py                                        \
     $APOLLO_DIR/docker-entrypoint.sh                           \
     $APOLLO_DIR/full_text_search_rabbit_consumer.py            \
     $APOLLO_DIR/full_text_search_analytic.py  $APP_DIR/

# 6.) clean-ups
RUN apt-get autoremove -y --purge && apt-get clean all

# 7.) start the app
CMD ["bash", "docker-entrypoint.sh"]
