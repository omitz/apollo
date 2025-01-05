from flask import Blueprint, Flask, current_app, jsonify, request
from flask_restful import Api, Resource, abort, reqparse
import json
import numpy as np

blueprint = Blueprint('api', __name__)
api = Api(blueprint)

class NumpyArrayEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, np.ndarray):
            return obj.tolist()
        return json.JSONEncoder.default(self, obj)

class HealthCheckResource(Resource):
    def get(self):
        return {'hello': 'world'}

class FindResource(Resource):
    def get(self):
        file_name = request.args['name']
        results = current_app.analytic.get_closest_results(file_name)
        dict_results = [result.as_dict() for result in results if result is not None]
        json_results = jsonify({'results': dict_results })

        return json_results

class URLMapResource(Resource):
    def get(self):
        from flask_url_map_serializer import dump_url_map
        return dump_url_map(current_app.url_map)


api.add_resource(HealthCheckResource, '/health')
api.add_resource(FindResource, '/find')


if current_app.config['DEBUG']:
    api.add_resource(URLMapResource, '/map/')
