from datetime import datetime

from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY, TIMESTAMP

from .ModelBase import ModelBase


class DetectedFace(ModelBase):
    __tablename__ = 'detected_faces'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    original_source = Column(String)
    ulx = Column(Float)
    uly = Column(Float)
    lrx = Column(Float)
    lry = Column(Float)
    probability = Column(Float)
    prediction = Column(String)
    vector_id = Column(BigInteger)
    query_results = Column(ARRAY(BigInteger))        
    timestamp = Column(TIMESTAMP(timezone=False), nullable=False, default=datetime.now())

    def __repr__(self):
        return "<DetectedFace  (id='%s', path='%s', vector_id='%s')>" % (self.id, self.path, self.vector_id)

    def serialize(self):
        return {
            "id": self.id,
            "path": self.path,
            "ulx": self.ulx,
            "uly": self.uly,
            "lrx": self.lrx,
            "lry": self.lry,
            "original_source": self.original_source,
            "probability": self.probability,
            "prediction": self.prediction,
            "vector_id": self.vector_id,
            "query_results": self.query_results,
            "timestamp": self.timestamp
        }
