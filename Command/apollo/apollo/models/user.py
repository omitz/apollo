import secrets

from sqlalchemy import Column, Integer, String, BigInteger, ARRAY
from sqlalchemy_utils import TSVectorType
from passlib.hash import sha256_crypt

from .ModelBase import ModelBase


class User(ModelBase):

    __tablename__ = "users"

    id = Column(BigInteger, primary_key=True, nullable=False)
    username = Column(String)
    password_hash = Column(String)
    roles = Column(ARRAY(String)) 

    def __init__(self, username, password_hash, roles): 
        self.username = username
        self.password_hash = password_hash
        self.roles = roles


    @property
    def rolenames(self):
        try:
            return self.roles
        except Exception:
            return []
    
    def verify_password(self, password_to_check):
        return sha256_crypt.verify(password_to_check, self.password_hash)

    def serialize(self):
        return {
            "id": self.id,
            "username": self.username,
            "roles": self.roles,
        }

    @property
    def __repr__(self):
        return f"<User  (id='{self.id}', username='{self.username}')>"

    @property
    def identity(self):
        return self.id

    @classmethod
    def lookup(cls, username):
        """
        look up user by username, can only be used within flask app context
        """

        from flask import current_app
        with current_app.app_context():
            from api import db
            return db.session.query(cls).filter(cls.username==username).scalar()
    
    @classmethod
    def identify(cls, id):
        """
        get identity, can only be used within flask app context
        """
        from flask import current_app
        with current_app.app_context():
            from api import db
            return db.session.query(cls).filter(cls.id==id).scalar()