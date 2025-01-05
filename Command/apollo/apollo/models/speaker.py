from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY

from .ModelBase import ModelBase


class Speaker(ModelBase):
    __tablename__ = 'speakers'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    original_source = Column(String)
    prediction = Column(String)
    score = Column(Float)
    vector_id = Column(BigInteger)
    mime_type = Column(String)

    def __repr__(self):
        return "<Speaker  (id='%s', path='%s', prediction='%s')>" % (self.id, self.path, self.prediction)

    def serialize(self):
        return {
            "id": self.id,
            "path": self.path,
            "original_source": self.original_source,
            "prediction": self.prediction,
            "score": self.score,
            "vector_id": self.vector_id,
        }