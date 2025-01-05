from flask import Blueprint, Flask, current_app, jsonify, request
from flask_restful import Resource, Api, reqparse, abort

search_blueprint = Blueprint('search', __name__)
api = Api(search_blueprint)