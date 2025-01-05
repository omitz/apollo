from apollo import PostgresDatabase, S3FileStore, models
from sqlalchemy.ext.serializer import dumps


def create_postgres_fixture():
    """
    Create a file representation of every table in apollo database
    and save to s3
    """
    
    db = PostgresDatabase()
    engine = db.get_or_create_engine()

    session = db.get_session()
    model_classes = dict([(name, cls) for name, cls in models.__dict__.items() if isinstance(cls, type)])
    dump = dict()
    for name in model_classes.keys():
        if name != "ModelBase":
            tablename = model_classes.get(name).__table__
            print(f"Tablename: {tablename}")
            if db.has_table(tablename):
                query = session.query(model_classes.get(name)).all()
        
                # pickle the query
                serialized = dumps(query)
                dump[name] = serialized
    
    with open('postgres_dump.txt', 'w') as f:
        for name in dump.keys():
            print(name)
            f.write(name)
            f.write('\n')
            print(str(dump[name]))
            f.write(str(dump[name]))
            f.write('\n')

    filestore = S3FileStore()
    filestore.upload_file('postgres_dump.txt', 'data_fixtures/postgres_dump.txt')
            

if __name__ == '__main__':
    create_postgres_fixture()