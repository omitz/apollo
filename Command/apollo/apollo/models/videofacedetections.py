from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY

from .ModelBase import ModelBase


class VideoFaceDetections(ModelBase):
    __tablename__ = 'video_face_detections'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    original_source = Column(String)
    prediction = Column(String)
    seconds = Column(ARRAY(Integer))
    recog_probability = Column(Float)
    
    def __repr__(self):
        return "<VideoFaceDetection  (id='%s', path='%s')>" % (self.id, self.path)

    def serialize(self):
        return {
            "id": self.id,
            "path": self.path,
            "prediction": self.prediction,
            "recog_probability": self.recog_probability,
            "seconds": self.seconds,
        }