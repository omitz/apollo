from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import MetaData

metadata = MetaData()
ModelBase = declarative_base(metadata=metadata)

def as_dict(self):
    return {c.name: getattr(self, c.name) for c in self.__table__.columns} 

setattr(ModelBase, "as_dict", as_dict)