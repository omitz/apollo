from flask import Blueprint, Flask, current_app, jsonify, request
from flask_restful import Resource, Api, reqparse, abort
from flask_praetorian import auth_required


api_blueprint = Blueprint('api', __name__)
api = Api(api_blueprint)

class HealthCheckResource(Resource):
    def get(self):
        return {'hello': 'Enrollment Creation'}

class URLMapResource(Resource):

    @auth_required
    def get(self):
        from flask_url_map_serializer import dump_url_map
        return dump_url_map(current_app.url_map)


api.add_resource(HealthCheckResource, '/')

if current_app.config['DEBUG']:
    api.add_resource(URLMapResource, '/map/')
