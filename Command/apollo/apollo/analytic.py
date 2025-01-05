import os

from abc import ABC, abstractmethod

from .filestore import FileStore
from .filemanager import FileManager


class Analytic(ABC):
    """Base class for Apollo analytics"""

    def __init__(self, name):
        self.name = name
        self.filemanager = FileManager()

    @abstractmethod
    def run(self, filename) -> dict:
        """
        Run the analytic on an input file
        @return: python dictionary representation of results
        """
        pass

    @abstractmethod
    def get_closest_results(self, file_name: str, num_results=10) -> dict:
        """
        Query for the top results in the database that are closest to file_name
        """
        pass

    @staticmethod
    def name(self) -> str:
        """
        The analytic name

        Returns:
            name: str, the analytic name
        """
        return self.name

    @abstractmethod
    def cleanup(self, filename):
        """
        delete local files specific to one job. It's recommended to include self.filemanager.cleanup() as part of any analytic's cleanup function.
        """
        pass

    def download_model(self, model_file_path: str, filestore: FileStore):
        """ download model file from filestorage """
        if not hasattr(self, 'model') or self.model is None:
            filestore.download_file(model_file_path, '/dev/shm')
            self.model = os.path.join('/dev/shm', os.path.basename(model_file_path))
        return self.model

    def get_closest_results(self, file_name: str, num_results=10) -> dict:
        """
        Query for the top results in the database that are closest to file_name

        Only some analytics need to implement this function. In the future, we might want to move this function to a child class. For now, we'll raise an exception to fail fast if this function is called when it hasn't been overridden.
        """
        raise NotImplementedError
