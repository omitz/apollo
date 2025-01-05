from datetime import datetime

from sqlalchemy import Column, Integer, Float, String, BigInteger, ARRAY, TIMESTAMP

from .ModelBase import ModelBase


class VideoDetections(ModelBase):
    __tablename__ = 'video_detections'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    original_source = Column(String)
    detection_class = Column(String)
    detection_score = Column(Float)
    seconds = Column(ARRAY(Integer))
    timestamp = Column(TIMESTAMP(timezone=False), nullable=False, default=datetime.now())

    def __repr__(self):
        return f"<VideoDetection  (id='{self.id}', path='{self.path}')>"
        
    def serialize(self):
        return {
            "id": self.id,
            "path": self.path,
            "original_source": self.original_source,
            "detection_class": self.detection_class,
            "detection_score": self.detection_score,
            "seconds": self.seconds, 
            "timestamp": self.timestamp,   
        }