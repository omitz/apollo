from flask import Flask

def create_app(config='config.Config'):
    print(" __init__.py: Enter create app()......", flush=True)
    app = Flask(__name__)
    app.url_map.strict_slashes = False
    app.config.from_object(config)

    with app.app_context():
        print(" __init__.py: Enter create app() with app.app_context()", flush=True)

        from .api import blueprint
        app.register_blueprint(blueprint)

        from .search import search_blueprint
        app.register_blueprint(search_blueprint, url_prefix='/search')

        return app





