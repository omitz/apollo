import os

from abc import ABC, abstractmethod

class RelationalDatabase(ABC):

    @abstractmethod
    def __init__(self, database_name: str = 'apollo'):
        """
        connect to database, create databases, create all tables
        """
        pass

    @abstractmethod
    def close(self):
        """
        close all connections
        """
        pass

    @abstractmethod
    def save_record_to_database(self, record, model):
        pass

    @abstractmethod
    def create_table_if_not_exists(self, table):
        pass

    @abstractmethod
    def create_database_if_not_exists(self):
        pass

    @abstractmethod
    def has_table(self, table):
        pass