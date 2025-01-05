from datetime import datetime

from sqlalchemy import Column, Integer, String, BigInteger, ARRAY, TIMESTAMP, DateTime, func

from .ModelBase import ModelBase

class EnrollmentLog (ModelBase):
    """Table schema for supporting enrollment log search.
         - user
         - timestamp
         - action
         - paramters
    """
    
    __tablename__ = 'enrollment'
    id = Column (BigInteger, primary_key=True, nullable=False)
    user = Column (String) # user login name
    # timestamp = Column (TIMESTAMP(timezone=False), nullable=False, default=datetime.now())
    timestamp = Column (String)
    action = Column (String) # action performed
    parameters = Column (String) # perameter supplied
    success = Column (String) # perameter supplied
    error = Column (String) # perameter supplied

    def __repr__(self):
        return (f"<Enrollment  (id='{self.id}', user='{self.user}', " +  
                f"timestamp='{self.timestamp})>")

    
    def serialize (self):
        """Serialize user query result.
        Args:
          search_user: The user to search

        Return:
          A dictionary.
        """
        # Package and serial the result:
        result = {
            "id":              self.id,
            "user":            self.user,
            "timestamp":       str(self.timestamp),
            "action":          self.action,
            "parameters":      self.parameters,
            "success":         self.success,
            "error":           self.error}

        # We are done.
        return result
