from datetime import datetime

from sqlalchemy import Column, Float, Integer, String, BigInteger, ARRAY, TIMESTAMP

from .ModelBase import ModelBase

class AnalyzeSentiment(ModelBase):
    __tablename__ = 'text_sentiment'

    id = Column (BigInteger, primary_key=True, nullable=False)
    path = Column (String)            # name of the image file
    original_source = Column (String)
    full_text = Column (String)
    sentiment = Column (String) # can be 'neutral', 'positive', or 'negative'
    polarity = Column (Float)   # 0 = neutral, < 0 = negative, > 0 = positive

    # identify the data source (eg., "speech_to_text")
    service_name = Column (String) # rabbit service name
    
    timestamp = Column(TIMESTAMP(timezone=False), nullable=False, default=datetime.now())

    def __repr__(self):
        return f"<AnalyzeSentiment  (id='{self.id}', path='{self.path}')>"

    def serialize(self):

        return {
            "id": self.id,
            "path": self.path,
            "service_name": self.service_name,
            "full_text": self.full_text,
            "sentiment": self.sentiment,
            "polarity": self.polarity,
            "original_source": self.original_source,
            "timestamp": self.timestamp
        }

