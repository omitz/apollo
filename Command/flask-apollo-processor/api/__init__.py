import os
import time

import pika
from flask import Flask
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm.exc import ObjectDeletedError
from flask_cors import CORS
from flask_swagger_ui import get_swaggerui_blueprint
from sqlalchemy.exc import OperationalError
from passlib.hash import sha256_crypt
from flask_praetorian import Praetorian

from commandutils import postgres_utils
from .publisher import Publisher
from apollo.models import metadata, User


db = SQLAlchemy(metadata=metadata)
guard = Praetorian()

def create_app(mock_search_services=False):
    """ 
    mock_search_services flag mocks calls to search microservices for testing purposes.
    This avoids requiring all search services to be built in order to test this flask app.
    This is currently only implemented in api/search.py SearchByEntity.
    To create app with mocked search services: cls.flask_app = create_app(mock_search_services=True)
    """

    app = Flask(__name__)

    ### swagger specific ###
    SWAGGER_URL = '/swagger'
    API_URL = '/static/apollo-api.yaml'
    SWAGGERUI_BLUEPRINT = get_swaggerui_blueprint(
        SWAGGER_URL,
        API_URL,
        config={
            'app_name': "flask-apollo-processor"
        }
    )
    app.register_blueprint(SWAGGERUI_BLUEPRINT, url_prefix=SWAGGER_URL)
    ### end swagger specific ###

    app.url_map.strict_slashes = False
    CORS(app)
    app.config.from_object('config.Config')
    app.config['JWT_ACCESS_LIFESPAN'] = {'hours': 24}
    app.config['JWT_REFRESH_LIFESPAN'] = {'days': 30}
    app.config['MOCK_SEARCH_SERVICES'] = mock_search_services
    guard.init_app(app, User)
    db.init_app(app)
    app.rmq_publisher = Publisher()

    with app.app_context():
        engine = get_postgres_engine()
        postgres_utils.create_database_if_not_exists(engine)

        # Create all the tables
        db.create_all()
        db.session.commit()

        from .api import api_blueprint
        app.register_blueprint(api_blueprint)

        from .jobs import jobs_blueprint
        app.register_blueprint(jobs_blueprint, url_prefix='/jobs')

        from .search import search_blueprint
        app.register_blueprint(search_blueprint, url_prefix='/search')

        from .upload import upload_blueprint
        app.register_blueprint(upload_blueprint, url_prefix='/upload')

        from .admin import admin_blueprint
        app.register_blueprint(admin_blueprint, url_prefix='/admin')

        from .auth import auth_blueprint
        app.register_blueprint(auth_blueprint)

        #TODO: remove once we have a way to create admin users
        users = [
            User("john user", sha256_crypt.hash("johnpassword"), ["user"]),
            User("susan admin", sha256_crypt.hash("susanpassword"), ["user", "admin"])
        ]

        for user in users:
            db_user = db.session.query(User).filter_by(username=user.username)
            try:
                db_user.delete()
            except ObjectDeletedError:
                pass
            db.session.add(user)
            db.session.commit()
        return app


def get_postgres_engine():
    try: 
        return postgres_utils.get_engine()
    except OperationalError as error:
        num_tries = 10
        i = 0
        for i in range(num_tries):
            app.logger.info("Retrying postgres connection: {i}/{num_tries}")
            try:
                return postgres_utils.get_engine()
                postgres_utils.create_database_if_not_exists(engine)
            except OperationalError as error:
                time.sleep(5)
            i = i + 1

        app.logger.info("Connected to postgres!")