import secrets

from flask import Blueprint, current_app, request
from flask_restful import Resource, Api
from passlib.hash import sha256_crypt
from flask_praetorian import roles_required, current_user

from apollo.models import User
from . import db
from . import guard


auth_blueprint = Blueprint('auth', __name__)
api = Api(auth_blueprint)

class UserResource(Resource):

    @roles_required("admin")
    def post(self):
        print(f"username {current_user().username}")
        json = request.get_json()
        user = User(json['username'], sha256_crypt.hash(json['password']), json['roles'])
        db.session.add(user)
        db.session.commit()


class LoginResource(Resource):

    def post(self):
        username = request.get_json()['username']
        password = request.get_json()['password'] 
        user = db.session.query(User).filter_by(username=username).scalar()
        
        if user and user.verify_password(password):
            user_s = user.serialize()
            jwt = guard.encode_jwt_token(user)
            return {'user': user_s, 'authorization_token': jwt}
        
        return {"error": "authentication failed"}, 401

api.add_resource(LoginResource, '/login/')
api.add_resource(UserResource, '/users/')