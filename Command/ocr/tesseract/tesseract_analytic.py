import os
from apollo import Analytic, S3FileStore, PostgresDatabase
from pathlib import Path
import shutil
import mpu.aws as mpuaws
from sqlalchemy import func
import tesseract_main

S3_OUTPUT_DIR = 'outputs/ocr/'
SERVICE_NAME = 'ocr_tesseract'

class TesseractAnalytic (Analytic):
    s3filestore = None
    ram_storage = '/dev/shm'
    
    def __init__(self, name):
        super().__init__(name)
        self.s3filestore = S3FileStore()


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
        ram_target_Path = Path(self.ram_storage) / Path(target).name
        
        #
        # OCR the image:
        #
        ram_outdir_Path = Path(self.ram_storage) / str(os.getpid())
        if ram_outdir_Path.is_dir():
            shutil.rmtree(ram_outdir_Path) # dangerous
        ram_outdir_Path.mkdir()            # could raise exception?
        output_Path = Path(Path(target).name.replace('.', '_') + "_ocr-tesseract.txt")
        ram_output_Path = ram_outdir_Path / output_Path
        # ram_metadata_Path = ram_outdir_Path / output_Path.name.replace (".txt", "_metadata.png")
        # TC 2020-10-28 (Wed) -- Change metadata file to be a json
        # file containing the bounding box (polygon) of each text.  We
        # can visualize it later using overlayPredictions.py
        ram_metadata_Path = ram_outdir_Path / output_Path.name.replace (".txt", "_metadata.json")

        print ("tesseract_main.run...", flush=True)
        succeed = tesseract_main.run (ram_target_Path, ram_output_Path, None, ram_metadata_Path)

        if not succeed:
            print ("ERROR: process_file Failed!", flush=True)
            self.cleanup (ram_target_Path)
            self.cleanup (ram_outdir_Path)
            return

        #
        # Save output files:
        #
        self.s3filestore.upload_dir_content (str (ram_outdir_Path), S3_OUTPUT_DIR)
        
        s3_bucket = self.s3filestore.s3_bucket # get the default s3 bucket 
        fulltext_Path = Path(s3_bucket) / S3_OUTPUT_DIR / ram_output_Path.name
        fulltext_s3_path = "s3://" +  str (fulltext_Path)
        metadata_Path = Path(s3_bucket) / S3_OUTPUT_DIR / ram_metadata_Path.name
        metadata_s3_path = "s3://" +  str (metadata_Path)
        
        #
        # Populate the database
        #
        # Read the text back..
        full_text = ram_output_Path.read_text()
        meta_data = ram_metadata_Path.read_text()

        # populate the record into the table:
        row = dict()
        row['path'] = s3_file_path
        row['fulltext_path'] = fulltext_s3_path
        row['full_text'] = full_text # Not necessary to add to db.  -- TODO

        clean_full_text = full_text.replace("'", "")
        row['search_vector'] = func.to_tsvector('english', clean_full_text)
        row['service_name'] = SERVICE_NAME

        # metadata are service dependent
        row['metadata_path'] = metadata_s3_path
        row['meta_data'] = meta_data # Not necessary to add to db.  -- TODO

        #
        # Clean ups and return database row
        #
        self.cleanup (ram_target_Path)
        self.cleanup (ram_outdir_Path)
        return row

        
    def get_closest_results(self, file_name: str, num_results=10) -> dict:
        """
        Query for the top results in the database that are closest to file_name.
        """
        pass

    
    def cleanup(self, fileOrDir_Path):
        print (f"cleaning up..", flush=True)
        if fileOrDir_Path.is_file():
            fileOrDir_Path.unlink()
            
        if fileOrDir_Path.is_dir():
            shutil.rmtree (fileOrDir_Path) # dangerous
    
