import os

class Config(object):
    # flask vars
    SECRET_KEY = os.environ.get('SECRET_KEY', 'developermode')
    DEBUG = os.environ.get('DEBUG', 0)
    TESTING = os.environ.get('TESTING', True)
    AWS_ACCESS_KEY_ID = os.environ.get('AWS_ACCESS_KEY_ID')
    AWS_SECRET_ACCESS_KEY = os.environ.get('AWS_SECRET_ACCESS_KEY')
    BUCKET_NAME = os.environ.get('BUCKET_NAME')
    NEO4J_HOST = os.environ.get('NEO4J_HOST', None)
    NEO4J_AUTH = os.environ.get('NEO4J_AUTH', None)
