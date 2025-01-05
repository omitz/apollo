from unittest import TestCase
import sqlalchemy
from sqlalchemy_utils import database_exists, create_database

from commandutils.models import *
from commandutils.postgres_utils import *



class TestPostgresUtils(TestCase):

    @classmethod
    def setUpClass(self):
        os.environ['POSTGRES_HOST'] = "postgres"
        os.environ['POSTGRES_PASSWORD'] = "secretpassword"
        

    def setUp(self):
        self.engine = get_engine(echo=False)
        self.session = get_session(self.engine)


    def tearDown(self):
        meta = sqlalchemy.MetaData(self.engine)
        #meta.reflect()
        for table in reversed(meta.sorted_tables):
            table.drop(self.engine)
        self.session.close()
        self.engine.dispose()
        

    @classmethod
    def tearDownClass(self):
        pass

    def test_get_engine(self):
        #get rid of engine/session from setup
        self.session.close()
        self.engine.dispose()

        self.engine = get_engine(echo=False)
        self.assertIsInstance(self.engine, sqlalchemy.engine.Engine)

    def test_get_session(self):
        #get rid of engine/session from setup
        self.session.close()
        self.engine.dispose()

        self.engine = get_engine(echo=False)
        self.session = get_session(self.engine)
        self.assertIsInstance(self.session, sqlalchemy.orm.Session)
        self.assertTrue(self.session.is_active)

    def test_create_database_if_not_exists(self):
        create_database_if_not_exists(self.engine)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))

    def test_create_database_if_not_exists_named(self):
        #create a Db with non-default name
        self.engine.dispose()
        self.engine = get_engine(database_name='test_database', echo=False)
        create_database_if_not_exists(self.engine)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/test_database"))
        
    def test_create_database_already_exists(self):
        #create database
        create_database_if_not_exists(self.engine)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))

        #call create again
        create_database_if_not_exists(self.engine)
        #no errors and check if db still exists
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))

    def test_create_table_if_not_exists(self):
        #create a few tables
        create_table_if_not_exists(self.engine, FileHash.__table__)
        create_table_if_not_exists(self.engine, DetectedFace.__table__)
        create_table_if_not_exists(self.engine, DetectedObj.__table__)
        create_table_if_not_exists(self.engine, Landmark.__table__)

        #check that all tables exist
        self.assertTrue(self.engine.dialect.has_table(self.engine, FileHash.__table__))
        self.assertTrue(self.engine.dialect.has_table(self.engine, DetectedFace.__table__))
        self.assertTrue(self.engine.dialect.has_table(self.engine, DetectedObj.__table__))
        self.assertTrue(self.engine.dialect.has_table(self.engine, Landmark.__table__))

    def test_create_table_already_exists(self):
        #create a few tables
        create_table_if_not_exists(self.engine, FileHash.__table__)
        create_table_if_not_exists(self.engine, DetectedFace.__table__)
        create_table_if_not_exists(self.engine, DetectedObj.__table__)
        create_table_if_not_exists(self.engine, Landmark.__table__)

        #call creates again
        create_table_if_not_exists(self.engine, FileHash.__table__)
        create_table_if_not_exists(self.engine, DetectedFace.__table__)
        create_table_if_not_exists(self.engine, DetectedObj.__table__)
        create_table_if_not_exists(self.engine, Landmark.__table__)

        #check that all tables exist and no errors thrown
        self.assertTrue(self.engine.dialect.has_table(self.engine, FileHash.__table__))
        self.assertTrue(self.engine.dialect.has_table(self.engine, DetectedFace.__table__))
        self.assertTrue(self.engine.dialect.has_table(self.engine, DetectedObj.__table__))
        self.assertTrue(self.engine.dialect.has_table(self.engine, Landmark.__table__))

    def test_init_database(self):
        init_database(FileHash, echo=False)

        #check db exists
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))
        #check table exists
        self.assertTrue(self.engine.dialect.has_table(self.engine, FileHash.__table__))

    def test_init_database_idempotent(self):
        self.engine = init_database(FileHash, echo=False)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))
        self.assertTrue(self.engine.dialect.has_table(self.engine, FileHash.__table__))

        #call init on new db
        self.engine = init_database(Landmark, db_name="test_database", echo=False)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/test_database"))
        self.assertTrue(self.engine.dialect.has_table(self.engine, Landmark.__table__))

        #call init on same database, different model
        self.engine = init_database(DetectedObj, echo=False)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))
        self.assertTrue(self.engine.dialect.has_table(self.engine, DetectedObj.__table__))
        #old table still exists
        self.assertTrue(self.engine.dialect.has_table(self.engine, FileHash.__table__))
    
        #call init on same database, same model
        self.engine = init_database(FileHash, echo=False)
        self.assertTrue(database_exists("postgresql+psycopg2://postgres:secretpassword@postgres/apollo"))
        #old tables still exist
        self.assertTrue(self.engine.dialect.has_table(self.engine, DetectedObj.__table__))
        self.assertTrue(self.engine.dialect.has_table(self.engine, FileHash.__table__))

    def test_save_record_to_database(self):
        DetectedObj.__table__.drop(self.engine)

        record = {
            "path" : "s3://apollo-source-data/fake/path/",
            "bb_ymin": float(0.003),
            "bb_xmin": float(0.999),
            "bb_ymax": float(0.21789),
            "bb_xmax": float(0.000000123),
            "detection_score": float(0.15),
            "detection_class": "cat"
        }

        record_model = DetectedObj(**record)
        self.engine = init_database(DetectedObj, echo=False)
        save_record_to_database(self.engine, record, DetectedObj)

        q = self.session.query(DetectedObj).filter(DetectedObj.detection_class == "cat")
        self.assertTrue(self.session.query(q.exists()).scalar())
        query_result = q.scalar()

        self.assertEqual(record_model.path, query_result.path)
        self.assertEqual(record_model.bb_ymin, query_result.bb_ymin)
        self.assertEqual(record_model.bb_xmax, query_result.bb_xmax)
        self.assertEqual(record_model.bb_ymax, query_result.bb_ymax)
        self.assertEqual(record_model.bb_xmin, query_result.bb_xmin)
        self.assertEqual(record_model.detection_score, query_result.detection_score)
        self.assertEqual(record_model.detection_class, query_result.detection_class)

