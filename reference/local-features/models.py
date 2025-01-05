import numpy as np
from sqlalchemy import Integer, ForeignKey, String, Column, Float, PickleType
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship

Base = declarative_base()


class Image(Base):
    __tablename__ = 'image'

    id = Column(Integer, primary_key=True)
    filepath = Column(String, unique=True)
    keypoints = Column(PickleType)
    descriptors = Column(PickleType)
