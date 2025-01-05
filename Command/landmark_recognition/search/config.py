import os

class Config(object):
    # flask vars
    SECRET_KEY = os.environ.get('SECRET_KEY', 'developermode')
    DEBUG = os.environ.get('DEBUG', 0)
    TESTING = os.environ.get('TESTING', True)
    POSTGRES_USER = os.environ.get('POSTGRES_USER', 'postgres')
    POSTGRES_PASSWORD = os.environ.get('POSTGRES_PASSWORD', 'secretpassword')
    POSTGRES_HOST = os.environ.get('POSTGRES_HOST', 'postgres')
    SQLALCHEMY_DATABASE_URI = os.environ.get('SQLALCHEMY_DATABASE_URI', 'postgresql+psycopg2://' + POSTGRES_USER + ':' + POSTGRES_PASSWORD + '@' + POSTGRES_HOST + ':5432/apollo')
    SQLALCHEMY_TRACK_MODIFICATIONS = os.environ.get('SQLALCHEMY_TRACK_MODIFICATIONS', False)
    AWS_ACCESS_KEY_ID = os.environ.get('AWS_ACCESS_KEY_ID')
    AWS_SECRET_ACCESS_KEY = os.environ.get('AWS_SECRET_ACCESS_KEY')
    BUCKET_NAME = os.environ.get('BUCKET_NAME')
