from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY

from .ModelBase import ModelBase


class FileHash(ModelBase):
    __tablename__ = 'file_hashes'
    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    sha1 = Column(String)
    sha256 = Column(String)
    sha512 = Column(String)
    md5 = Column(String)

    def __repr__(self):
        return f"<FileHash  (id='{self.id}', path='{self.path}', sha1='{self.sha1})>"