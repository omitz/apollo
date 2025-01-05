from abc import ABC, abstractmethod

class FileStore(ABC):

    """ Base class for file store implementation such as S3 """
    def __init__(self):
        pass

    @abstractmethod
    def download_file(self, file_path: str, outdir: str):
        pass

    @abstractmethod
    def upload_file(self, local_outfile, upload_path):
        pass

    @abstractmethod
    def delete_file(self, file_path: str):
        pass