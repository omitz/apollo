import os
import warnings
from apollo import Analytic, S3FileStore, PostgresDatabase
from pathlib import Path
import mpu.aws as mpuaws
from sqlalchemy import func

import textract
import magic

S3_OUTPUT_DIR = 'outputs/full_text_search'
SERVICE_NAME = 'full_text_search'

class FullTextSearchAnalytic (Analytic):
    s3filestore = None
    ram_storage = '/dev/shm'
    
    def __init__(self, name, testing_in_jenkins=False):
        super().__init__(name)
        if not testing_in_jenkins:
            self.s3filestore = S3FileStore()

        pass


    def run (self, s3_file_path: str):
        """Processes file and generates analytics.

        Args:
          s3_file_path:
            A s3 path.  eg.,"s3://apollo-source-data/input/audio/bill_gates-TED.mp3".

        Returns:
          The output of this function is passed to RabbitConsumer.save_results_to_database().
        """

        #
        # Access S3 bucket and save file to ram disk:
        #
        bucket, target = mpuaws._s3_path_split (s3_file_path)
        self.s3filestore.download_file (target, self.ram_storage)
        ram_target_path = os.path.join(self.ram_storage, os.path.basename(target))

        #
        # Populate the database
        #

        full_text = self.check_mime_and_read(ram_target_path)

        row = dict()
        row['path'] = s3_file_path
        row['fulltext_path'] = s3_file_path
        row['full_text'] = full_text # Not necessary to add to db.  -- TBF
        
        clean_full_text = full_text.replace("'", "")
        row['search_vector'] = func.to_tsvector('english', clean_full_text)
        row['service_name'] = SERVICE_NAME

        # metadata are service dependent
        row['metadata_path'] = None
        row['meta_data'] = None # Not necessary to add to db.  -- TBF

        #
        # Clean ups
        #
        self.cleanup (ram_target_path)

        return row

    def check_mime_and_read(self, ram_target_path):
        # Determine mime type
        file_checker = magic.Magic(mime=True)
        mime = file_checker.from_file(ram_target_path)
        # Read the text
        full_text = self.load_and_clean_doc(ram_target_path, mime)
        return full_text

    def load_and_clean_doc(self, filepath, filetype):
        # TODO This is a duplicate of a function in NER details. Maybe implement a subclass for text analytics that contains this function.
        print(f'text file: {filepath}\nfiletype: {filetype}', flush=True)
        if filetype == 'text/plain':
            with open(filepath, 'r') as file:
                text = file.read()
        elif filetype in ['application/msword',
                          'application/vnd.openxmlformats-officedocument.wordprocessingml.document']:  # doc and docx
            if filetype == 'application/msword':
                ext = 'doc'  # textract uses antiword
            else:
                ext = 'docx'  # textract uses python-docx2txt
            try:
                text = textract.process(filepath, extension=ext)
                text = text.decode("utf-8")
            except textract.exceptions.ShellError as e: # TODO This error can be thrown when a very short .doc or .docx created by OpenOffice is processed. For now, we'll assume that a doc with this little text is irrelevant. It'd be better to find an alternate way to read the files (maybe by passing it as an image to ocr-tesseract).
                print(e, flush=True)
                text = ''
        elif filetype == 'application/pdf':
            text = textract.process(filepath, extension='pdf')  # textract uses pdftotext
            text = text.decode("utf-8")
        else:
            warnings.warn('Unknown or new filetype. Attempting to read.')
            with open(filepath, 'r') as file:
                text = file.read()
        text = text.replace(u'\uFFFD', '')
        return text
    
    def get_closest_results(self, file_name: str, num_results=10) -> dict:
        """
        Query for the top results in the database that are closest to file_name.
        """
        pass
    
    def cleanup(self, filepath):
        if os.path.exists(filepath):
            print(f'cleaning up {filepath}', flush=True)
            os.remove(filepath)
