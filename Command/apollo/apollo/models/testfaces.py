from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY

from .ModelBase import ModelBase


class TestFaces(ModelBase):
    __tablename__ = 'test_faces'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    vector_id = Column(BigInteger)
    query_results = Column(ARRAY(BigInteger))

    def __repr__(self):
        return "<test  (id='%s', path='%s')>" % (self.id, self.path)