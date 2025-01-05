import unittest
import os

from passlib.hash import sha256_crypt

from apollo import PostgresDatabase, Neo4jGraphDatabase
from apollo.models import User
from api import create_app


class TestAuth(unittest.TestCase):

    open_endpoints = [
        ("GET", "/"), #health check endpoint
    ]

    user_endpoints = [
        #("GET", "map"),
        #searches
        ("GET", "/search/tag/?tag=dog"),
        ("GET", "search/tag_video/?tag=dog"),
        ("GET", "search/scene_hierarchy/?tag=outdoor"),
        ("GET", "search/scene_class/?tag=restaurant"),
        ("GET", "search/ner/?entity=china"),
        ("GET", "search/full_text/?query=test"),
        ("GET", "/search/get_signed_url/?key=s3://apollo-source-data/input/audio/bill_gates-TED.mp3"),
        ("GET", "search/check_database/?path=s3://apollo-source-data/input/audio/bill_gates-TED.mp3'&model=SearchFullText"),
    ]

    admin_endpoints = [
        #("GET", "/admin/create_fixtures/"),
        #("GET", "admin/restore_databases/"), # I don't want to call these endpoints because they will change the state of the DB and s3
        ("POST", "users/", '{"username":"testuser", "password":"testpassword", "roles": ["user"]}')
    ]

    @classmethod
    def setUpClass(cls):
        cls.flask_app = create_app(mock_search_services=True)
        cls.flask_app.app_context().push()
        os.environ['POSTGRES_HOST'] = 'postgres'
        os.environ['POSTGRES_PASSWORD'] = 'secretpassword'
        os.environ['NEO4J_HOST'] = 'neo4j'
        os.environ['NEO4J_AUTH'] = 'neo4j/neo4j-password'
        cls.database = PostgresDatabase(table=User.__table__)
        cls.database.delete_all_from_table(User)
        cls.neo4j = Neo4jGraphDatabase()
        cls.neo4j.wait_until_available()

        with cls.flask_app.app_context():
            from api import db
            
            if not db.session.query(User).filter_by(username="user").first():
                non_admin_user = User("user", sha256_crypt.hash("user"), ["user"])
                db.session.add(non_admin_user)
                db.session.commit()

            if not db.session.query(User).filter_by(username="admin").first():
                admin_user = User("admin", sha256_crypt.hash("admin"), ["user", "admin"])
                db.session.add(admin_user)
                db.session.commit()
    
    @classmethod
    def tearDownClass(cls):
        cls.database.delete_all_from_table(User)
        cls.database.close()

    def test_unauthenticated(cls):
        with cls.flask_app.test_client() as client:
            for endpoint in cls.open_endpoints:
                response = cls.make_request(client, endpoint)
                cls.assertEqual(200, response._status_code)

            for endpoint in cls.user_endpoints:
                response = cls.make_request(client, endpoint)
                cls.assertEqual(401, response._status_code)

            for endpoint in cls.admin_endpoints:
                response = cls.make_request(client, endpoint)
                cls.assertEqual(401, response._status_code)

    def test_non_admin_user(cls):
        with cls.flask_app.test_client() as client:
           
            #login as user
            token = cls.make_request(client, ("POST", "/login/", '{"username": "user", "password": "user"}')).json['authorization_token']
            
            for endpoint in cls.open_endpoints:
                response = cls.make_request(client, endpoint, token)
                cls.assertEqual(200, response._status_code)

            for endpoint in cls.user_endpoints:
                response = cls.make_request(client, endpoint, token)
                cls.assertEqual(200, response._status_code)

            for endpoint in cls.admin_endpoints:
                response = cls.make_request(client, endpoint, token)
                cls.assertEqual(403, response._status_code)

    def test_admin_user(cls):
        with cls.flask_app.test_client() as client:
           
            #login as admin user
            token = cls.make_request(client, ("POST", "/login/", '{"username": "admin", "password": "admin"}')).json['authorization_token']
            
            for endpoint in cls.open_endpoints:
                response = cls.make_request(client, endpoint, token)
                cls.assertEqual(200, response._status_code)

            for endpoint in cls.user_endpoints:
                response = cls.make_request(client, endpoint, token)
                cls.assertEqual(200, response._status_code)

            for endpoint in cls.admin_endpoints:
                response = cls.make_request(client, endpoint, token)
                cls.assertEqual(200, response._status_code)

    def make_request(cls, client, endpoint, authorization_token=None):
        
        headers = {}
        if authorization_token:
            headers['Authorization'] = f"Bearer {authorization_token}"

        if endpoint[0].upper() == "PUT":
            headers['Content-Type'] = "application/json"
            return client.put(endpoint[1], data=endpoint[2], headers=headers)
        elif endpoint[0].upper() == "POST":
            headers['Content-Type'] = "application/json"
            return client.post(endpoint[1], data=endpoint[2], headers=headers)
        elif endpoint[0].upper() == "DELETE":
            return client.delete(endpoint[1], headers=headers)
        elif endpoint[0].upper() == "OPTIONS":
            return client.options(endpoint[1], headers=headers)
        elif endpoint[0].upper() == "HEAD":
            return client.head(endpoint[1], headers=headers)
        else:
            #TODO: implement UPDATE, PATCH etc
            return client.get(endpoint[1], headers=headers)