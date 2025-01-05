from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker

from sqlalchemy_utils import database_exists, create_database

import os, sys

def check_processed(s3filepath, session, model):
    # Check the postgres table to see if we've processed this image before
    query = select_rows_with_img_path(s3filepath, session, model)
    # Get the query results as a list of tuples
    query_res = query.all()
    if len(query_res) == 0:
        return False
    else:
        return True

    
def create_table_if_not_exists(engine, table):
    if not engine.dialect.has_table(engine, table):
        table.create(engine)


def create_database_if_not_exists(engine):
    if not database_exists(engine.url):
        create_database(engine.url)


def get_engine(database_name='apollo', echo=True, pool_size=5, max_overflow=10):
    '''
    Args:
        echo: Whether or not to have SQL Alchemy logging
    '''
    password = os.getenv("POSTGRES_PASSWORD", "")
    host = os.getenv("POSTGRES_HOST", "postgres")
    connection_string = 'postgresql+psycopg2://postgres:' + password + '@'+ host + '/' + database_name
    engine = create_engine(connection_string, echo=echo, pool_size=pool_size, max_overflow=max_overflow)
    return engine


def get_session(engine):
    Session = sessionmaker(expire_on_commit=False)
    Session.configure(bind=engine)
    session = Session()
    return session


def init_database(model, db_name='apollo', echo=True):
    engine = get_engine(db_name, echo=echo)
    create_database_if_not_exists(engine)
    create_table_if_not_exists(engine, model.__table__)
    return engine


def save_record_to_database(engine, record, model, verbose=True):
    instance = model(**record)
    session = get_session(engine)
    session.add(instance)
    session.commit()
    if verbose:
        print ("Record: %s inserted successfully into %s table" % (instance.__repr__(), model.__tablename__))
    print('Closing session', flush=True)
    session.close()


def select_rows_with_img_path(s3filepath, session, model):
    query = session.query(model.id).filter(model.path == s3filepath)
    return query


# This function isn't currently necessary, since a unique id will automatically generated. However, I'll leave it here in case we want to perform this check in the future.
def get_taken_ids(database_name, model_class):
    taken_ids = set()
    engine = get_engine(database_name)
    session = get_session(engine)
    for query_result in session.query(model_class.id):
        taken_ids.add(query_result.id)
    return taken_ids