from datetime import datetime

from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY, TIMESTAMP

from .ModelBase import ModelBase


class DetectedObj(ModelBase):
    __tablename__ = 'detected_objs'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    original_source = Column(String, nullable=True)
    bb_ymin_xmin_ymax_xmax = Column(ARRAY(Float))
    detection_scores = Column(ARRAY(Float))
    detection_class = Column(String)
    timestamp = Column(TIMESTAMP(timezone=False), nullable=False, default=datetime.now())

    def __repr__(self):
        return "<DetectedObject  (id='%s', path='%s')>" % (self.id, self.path)

    def serialize(self):
        return {
            "id": self.id,
            "path": self.path,
            "original_source": self.original_source,
            "bb_ymin_xmin_ymax_xmax": [coordinate for coordinate in self.bb_ymin_xmin_ymax_xmax],
            "detection_scores": [score for score in self.detection_scores],
            "detection_class": self.detection_class,
            "timestamp": self.timestamp
        }