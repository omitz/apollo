from .create_milvus_fixture import create_milvus_fixture
#from .create_neo4j_fixture import create_neo4j_fixture
from .create_postgres_fixture import create_postgres_fixture
from .dump_milvus import dump_milvus
#from .dump_neo4j import dump_neo4j
from .dump_postgres import dump_postgres
from .load_milvus_fixture import load_milvus_fixture
#from .load_neo4j_fixture import load_neo4j_fixture
from .load_postgres_fixture import load_postgres_fixture


def create_fixtures():
    create_postgres_fixture()
    create_neo4j_fixture()
    create_milvus_fixture()

def dump_databases():
    dump_postgres()
    dump_neo4j()
    dump_milvus()

def load_fixtures():
    load_postgres_fixture()
    load_neo4j_fixture()
    load_milvus_fixture()