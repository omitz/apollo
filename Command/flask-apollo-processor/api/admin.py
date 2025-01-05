from flask import Blueprint, Flask, Blueprint, current_app 
from flask_restful import Resource, Api
from flask_praetorian import roles_required

from apollo.fixtures import create_fixtures, dump_databases, load_fixtures


admin_blueprint = Blueprint('admin', __name__)
api = Api(admin_blueprint)

class RestoreDatabasesResource(Resource):

    @roles_required("admin")
    def get(self):
        dump_databases()
        load_fixtures()
        return {'success': True}

class CreateFixtureResource(Resource):

    @roles_required("admin")
    def get(self):
        create_fixtures()
        return {'success': True}


if current_app.config['DEBUG']:
    api.add_resource(CreateFixtureResource, '/create_fixtures/')
    api.add_resource(RestoreDatabasesResource, '/restore_databases/')