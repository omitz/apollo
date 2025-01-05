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
    RABBITMQ_HOST = os.environ.get('RABBITMQ_HOST')
    RABBITMQ_USER = os.environ.get('RABBITMQ_USER')
    RABBITMQ_PASSWORD = os.environ.get('RABBITMQ_PASSWORD')
    AWS_ACCESS_KEY_ID = os.environ.get('AWS_ACCESS_KEY_ID')
    AWS_SECRET_ACCESS_KEY = os.environ.get('AWS_SECRET_ACCESS_KEY')
    BUCKET_NAME = os.environ.get('BUCKET_NAME')
    NEO4J_HOST = os.environ.get('NEO4J_HOST')
    NEO4J_AUTH = os.environ.get('NEO4J_AUTH')
    FACE_SEARCH_HOST=os.environ.get('FACE_SEARCH_HOST')
    LANDMARK_SEARCH_HOST=os.environ.get('LANDMARK_SEARCH_HOST')
    NER_SEARCH_HOST=os.environ.get('NER_SEARCH_HOST')
    SPEAKER_SEARCH_HOST=os.environ.get('SPEAKER_SEARCH_HOST')

    RABBITMQ_SUBSCRIBERS = [
        'facenet',
        'file_type',
        'landmark',
        'named_entity_recognition',
        'object_detection',
        'speaker_recognition',
        'speech_to_text',
        'virus_scanner',
        'scene_places365',
    ]

    RABBITMQ_EXCHANGE = 'ApolloExchange'
