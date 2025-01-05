import os
from unittest import TestCase

import sqlalchemy
from sqlalchemy_utils import database_exists, drop_database

from apollo.models import DetectedObj, DetectedFace, Landmark, TestFaces, FileHash
from apollo import PostgresDatabase


class TestPostgresDatabase(TestCase):

    @classmethod
    def setUpClass(self):
        os.environ['POSTGRES_HOST'] = "postgres"
        os.environ['POSTGRES_USER'] = "postgres"
        os.environ['POSTGRES_PASSWORD'] = "secretpassword"
        
    def setUp(self):
        self.postgresdatabase = PostgresDatabase(database_name='apollo', table=DetectedObj.__table__)

    def tearDown(self):
        engine = self.postgresdatabase.get_or_create_engine()
        meta = sqlalchemy.MetaData(engine)

        for table in reversed(meta.sorted_tables):
            table.drop(engine)
        
        user = os.getenv('POSTGRES_USER')
        password = os.getenv('POSTGRES_PASSWORD')
        host = os.getenv('POSTGRES_HOST')

        if database_exists(f"postgresql+psycopg2://{user}:{password}@{host}/test_database"):
            drop_database(f"postgresql+psycopg2://{user}:{password}@{host}/test_database")
        if database_exists(f"postgresql+psycopg2://{user}:{password}@{host}/apollo"):
            drop_database(f"postgresql+psycopg2://{user}:{password}@{host}/apollo")
       
        self.postgresdatabase.close()

    @classmethod
    def tearDownClass(self):
        pass
    
    def test_init(self):
        #db initiated in setUp()
        engine = self.postgresdatabase.get_or_create_engine()
        self.assertTrue(database_exists(engine.url))
        self.assertTrue(self.postgresdatabase.has_table(DetectedObj.__table__))

    def test_get_or_create_engine(self):
        engine = self.postgresdatabase.get_or_create_engine()
        self.assertIsInstance(engine, sqlalchemy.engine.Engine)

    def test_create_engine_default(self):
        engine = PostgresDatabase.create_engine()
        self.assertIsInstance(engine, sqlalchemy.engine.Engine)
        #password is masked
        self.assertEqual("apollo", engine.url.database)
        engine.dispose()

    def test_create_engine_named_database(self):
        engine = PostgresDatabase.create_engine(database_name='test_database')
        self.assertIsInstance(engine, sqlalchemy.engine.Engine)
        #password is masked
        self.assertEqual("test_database", engine.url.database)
        engine.dispose()

    def test_create_database_if_not_exists(self):
        self.assertFalse(
            database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/test_database")
        )
        
        self.postgresdatabase.close()
        engine = PostgresDatabase.create_engine(database_name='test_database')
        self.postgresdatabase = PostgresDatabase(engine=engine)
        
        self.postgresdatabase.create_database_if_not_exists()
        self.assertTrue(
            database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/test_database")
        )

        #idempotency
        self.postgresdatabase.create_database_if_not_exists()
        self.assertTrue(
            database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/test_database")
        )

    def test_create_database_already_exists(self):
        #call create again
        self.postgresdatabase.create_database_if_not_exists()
        #no errors and check if db still exists
        self.assertTrue(
            database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo")
        )

    def test_get_session(self):
        session = self.postgresdatabase.get_session()
        self.assertIsInstance(session, sqlalchemy.orm.Session)
        self.assertTrue(session.is_active)
        session.close()

    def test_create_table_if_not_exists(self):
        #create a few tables
        self.postgresdatabase.create_table_if_not_exists(FileHash.__table__)
        self.postgresdatabase.create_table_if_not_exists(DetectedFace.__table__)
        self.postgresdatabase.create_table_if_not_exists(DetectedObj.__table__)
        self.postgresdatabase.create_table_if_not_exists(Landmark.__table__)

        #check that all tables exist
        self.assertTrue(self.postgresdatabase.has_table(FileHash.__table__))
        self.assertTrue(self.postgresdatabase.has_table(DetectedFace.__table__))
        self.assertTrue(self.postgresdatabase.has_table(DetectedObj.__table__))
        self.assertTrue(self.postgresdatabase.has_table(Landmark.__table__))

    def test_create_table_already_exists(self):
        #create a few tables
        self.postgresdatabase.create_table_if_not_exists(FileHash.__table__)
        self.postgresdatabase.create_table_if_not_exists(DetectedFace.__table__)
        self.postgresdatabase.create_table_if_not_exists(DetectedObj.__table__)
        self.postgresdatabase.create_table_if_not_exists(Landmark.__table__)

        #call creates again
        self.postgresdatabase.create_table_if_not_exists(FileHash.__table__)
        self.postgresdatabase.create_table_if_not_exists(DetectedFace.__table__)
        self.postgresdatabase.create_table_if_not_exists(DetectedObj.__table__)
        self.postgresdatabase.create_table_if_not_exists(Landmark.__table__)

        #check that all tables exist and no errors thrown
        self.assertTrue(self.postgresdatabase.has_table(FileHash.__table__))
        self.assertTrue(self.postgresdatabase.has_table(DetectedFace.__table__))
        self.assertTrue(self.postgresdatabase.has_table(DetectedObj.__table__))
        self.assertTrue(self.postgresdatabase.has_table(Landmark.__table__))

    def test_init_database_idempotent(self):
        #call init on new db
        db2 = PostgresDatabase(database_name="test_database", echo=False, table=Landmark.__table__)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/test_database"))
        self.assertTrue(db2.has_table(Landmark.__table__))

        #call init on same database, different model
        db3 = PostgresDatabase(database_name='apollo', echo=False, table=FileHash.__table__)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))
        self.assertTrue(self.postgresdatabase.has_table(FileHash.__table__))
        #old table still exists
        self.assertTrue(self.postgresdatabase.has_table(DetectedObj.__table__))
    
        #call init on same database, same model
        db4 = PostgresDatabase(database_name="apollo", table=DetectedObj.__table__, echo=False)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))
        #old tables still exist
        self.assertTrue(self.postgresdatabase.has_table(DetectedObj.__table__))
        self.assertTrue(self.postgresdatabase.has_table(FileHash.__table__))

        db2.close()
        db3.close()
        db4.close()

    def test_save_record_to_database(self):
        record = {
            "path" : "s3://apollo-source-data/fake/path/",
            "original_source": "s3://apollo-source-data/fake/path/",
            "bb_ymin_xmin_ymax_xmax": [float(0.003), float(0.999), float(0.21789), float(0.000000123)],
            "detection_scores": [float(0.15)],
            "detection_class": "cat"
        }

        record_model = DetectedObj(**record)
        self.postgresdatabase.save_record_to_database(record, DetectedObj)

        session = self.postgresdatabase.get_session()
        q = session.query(DetectedObj).filter(DetectedObj.detection_class == "cat")
        self.assertTrue(session.query(q.exists()).scalar())
        query_result = q.scalar()

        self.assertEqual(record_model.path, query_result.path)
        self.assertEqual(record_model.original_source, query_result.original_source)
        self.assertEqual(record_model.bb_ymin_xmin_ymax_xmax, query_result.bb_ymin_xmin_ymax_xmax)
        self.assertEqual(record_model.detection_scores, query_result.detection_scores)
        self.assertEqual(record_model.detection_class, query_result.detection_class)

        session.close()

    def test_query(self):
        # Add a couple records to the objects table
        self.postgresdatabase.create_table_if_not_exists(DetectedObj.__table__)
        record = {
            "path" : "s3://apollo-source-data/fake/cat/",
            "original_source": "s3://apollo-source-data/fake/cat/",
            "bb_ymin_xmin_ymax_xmax": [float(0.003), float(0.999), float(0.21789), float(0.000000123)],
            "detection_scores": [float(0.15)],
            "detection_class": "cat"
        }
        self.postgresdatabase.save_record_to_database(record, DetectedObj)
        record = {
            "path" : "s3://apollo-source-data/fake/dog/",
            "original_source": "s3://apollo-source-data/fake/dog/",
            "bb_ymin_xmin_ymax_xmax": [float(0.003), float(0.999), float(0.21789), float(0.000000123)],
            "detection_scores": [float(0.15)],
            "detection_class": "dog"
        }
        self.postgresdatabase.save_record_to_database(record, DetectedObj)

        # Query
        result = self.postgresdatabase.query(DetectedObj, DetectedObj.detection_class == 'dog').first()
        print(f'result: {result}', flush=True)
        self.assertEqual(result.path, "s3://apollo-source-data/fake/dog/")

