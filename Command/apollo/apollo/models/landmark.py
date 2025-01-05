from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY

from .ModelBase import ModelBase


class Landmark(ModelBase):
    __tablename__ = 'landmarks'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    original_source = Column(String)
    delf_locations = Column(ARRAY(Float))
    delf_descriptors = Column(ARRAY(Float))

    def __repr__(self):
        return "<Landmark  (id='%s', path='%s')>" % (self.id, self.path)

    def serialize(self):
        return {
            "id": self.id,
            "path": self.path,
            "original_source": self.original_source
        }