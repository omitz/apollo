import ast

from sqlalchemy.ext.serializer import loads
from sqlalchemy import MetaData

from apollo import S3FileStore, PostgresDatabase, models

       
def load_postgres_fixture():
    """
    download postgres file dump from s3 and load into clean database
    """

    filestore = S3FileStore()
    filestore.download_file('data_fixtures/postgres_dump.txt', './')
    
    model_classes = dict([(name, cls) for name, cls in models.__dict__.items() if isinstance(cls, type)])

    db = PostgresDatabase(echo=True)
    engine = db.get_or_create_engine()
    metadata = MetaData(bind=engine)

    with open('postgres_dump.txt') as f:
        lines = f.readlines()
        count = 0
        for line in lines:
            #even lines are model names
            #odd lines are byte string representations dumps of a table

            if count % 2 == 0:
                print(line)
                modelclass = model_classes.get(line[:len(line) - 1])
                db.create_table_if_not_exists(modelclass.__table__)
                tablename = modelclass.__tablename__
            else:
                session = db.get_session()
                byte_str = ast.literal_eval(line[:len(line) - 1])
                #load query objects from byte string into a query object
                query = loads(byte_str, metadata, session)
                #bulk insert into db
                session.bulk_insert_mappings(modelclass, [model_instance.__dict__ for model_instance in query])
                session.execute(f"ALTER SEQUENCE {tablename}_id_seq RESTART WITH {len(query) + 1}")
                session.commit()
                session.close()

            count = count + 1    

if __name__ == "__main__":
    load_postgres_fixture()   