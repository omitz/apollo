import os

from sqlalchemy import create_engine


def dump_postgres():
    """
    drop apollo database
    """

    #drop database that may have active connections
    print("dropping apollo database")
    password = os.getenv("POSTGRES_PASSWORD", "")
    host = os.getenv("POSTGRES_HOST", "postgres")
    connection_string = 'postgresql+psycopg2://postgres:' + password + '@'+ host + '/postgres'
    engine = create_engine(connection_string)
    conn = engine.connect()
    conn.execute("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'apollo';")
    #conn.execute("UPDATE pg_database SET datallowconn = 'false' WHERE datname = 'apollo';")
    conn.execution_options(isolation_level="AUTOCOMMIT").execute("drop database if exists apollo;")    
    conn.close()
    engine.dispose()

if __name__ == "__main__":
    dump_postgres()