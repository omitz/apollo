import os

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy_utils import database_exists, create_database
from sqlalchemy.schema import Table
from sqlalchemy.engine import Engine

from .relationaldatabase import RelationalDatabase
from .models import ModelBase


class PostgresDatabase(RelationalDatabase):

        
    @staticmethod
    def create_engine(database_name='apollo', echo: bool = True, pool_size=5, max_overflow=10):

        password = os.getenv("POSTGRES_PASSWORD", "")
        host = os.getenv("POSTGRES_HOST", "postgres")
        connection_string = 'postgresql+psycopg2://postgres:' + password + '@'+ host + '/' + database_name
        engine = create_engine(connection_string, echo=echo, pool_size=pool_size, max_overflow=max_overflow)
        return engine

    def __init__(self, database_name: str = 'apollo', table: Table = None, engine: Engine = None, echo: bool = False):
       
        """
        connect to database, create databases, create all tables
        """
        if engine:
            self.engine = engine
        else:
            self.engine = self.get_or_create_engine(database_name=database_name, echo=echo)
        
        self.create_database_if_not_exists()
        
        if table is not None:
            self.create_table_if_not_exists(table)


    def check_processed(self, s3filepath, model, servicename = None):
        # Check the postgres table to see if we've processed this data before.
        session = self.get_session()
        if servicename:
            query = session.query(model.id).filter(model.path == s3filepath).filter(
                model.service_name == servicename)
        else:
            query = session.query(model.id).filter(model.path == s3filepath)
        # Get the query results as a list of tuples
        query_res = query.all()
        if len(query_res) == 0:
            return False
        else:
            return True

            
    def save_record_to_database(self, record: dict, model: ModelBase):
        """
        model: SQLAlchemy model class that inherits from SQLALchemy declarative_base
        record: a dict with k,v pairs of all the model class' attributes

        saves record to the database
        """

        instance = model(**record)
        session = self.get_session()
        session.expire_on_commit = False
        session.add(instance)
        session.commit()
        session.expunge(instance)
        print (f"Record: {instance.__repr__()} inserted successfully into {model.__tablename__} table")
        session.close()
  
    def create_table_if_not_exists(self, table):
        if not self.engine.dialect.has_table(self.engine, table):
            table.create(self.engine)

    def has_table(self, table):
        return self.engine.dialect.has_table(self.engine, table)

    def create_database_if_not_exists(self):
        if not database_exists(self.engine.url):
            create_database(self.engine.url)

    def get_or_create_engine(self, database_name: str = 'apollo', echo: bool = False):
        """
        Args:
            echo: Whether or not to have SQL Alchemy logging
        """
        
        if hasattr(self, 'engine') and self.engine:
            return self.engine
        else:
            return PostgresDatabase.create_engine(database_name=database_name, echo=echo)
    
    def get_session(self):
        Session = sessionmaker()
        Session.configure(bind=self.engine)
        session = Session()
        return session

    def close(self):
        self.engine.dispose()

    def delete_all_from_table(self, model):
        '''
        Delete all rows from the given model's table. Used for testing.
        '''
        session = self.get_session()
        session.query(model).delete()
        session.commit()
        session.close()

    def query(self, model: ModelBase, condition=None):
        session = self.get_session()
        if condition is not None:
            result = session.query(model).filter(condition)
        else:
            result = session.query(model)
        session.close()
        return result

    def get_all(self, model: ModelBase):
        session = self.get_session()
        result = session.query(model).all()
        return result
