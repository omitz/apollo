import os

from flask import Flask
from flask import current_app


from ...analytic import Analytic
from ..config import Config
from flask_cors import CORS
from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

def create_app(analytic: Analytic):
    print(" __init__.py: Enter create app()......", flush=True)
    app = Flask(__name__)
    app.url_map.strict_slashes = False
    CORS(app)
    app.config.from_object(Config)
    db.init_app(app)

    app.analytic = analytic

    with app.app_context():
        print(" __init__.py: Enter create app() with app.app_context()", flush=True)

        from .api import blueprint
        app.register_blueprint(blueprint)

        from .search import search_blueprint
        app.register_blueprint(search_blueprint, url_prefix='/search')

        return app
