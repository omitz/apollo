import os
from apollo import Analytic, S3FileStore
import mpu.aws as mpuaws
from pathlib import Path
import shutil
import textblob_main

S3_OUTPUT_DIR = 'outputs/sentiment_analysis'
SERVICE_NAME = 'text_sentiment'


class TextBlobAnalytic (Analytic):
    s3filestore = None
    ram_storage = '/dev/shm'

    def __init__(self, name):
        super().__init__(name)
        self.s3filestore = S3FileStore()

    def run (self, s3_file_path: str):
        """Processes file and generates analytics.

        Args:
          s3_file_path:
            A s3 path.  eg.,"s3://apollo-source-data/inputs/sentiment-analysis/positive.txt 

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
        # Sentiment Analysis
        #
        ram_outdir_Path = Path(self.ram_storage) / str(os.getpid())
        if ram_outdir_Path.is_dir():
            shutil.rmtree(ram_outdir_Path) # dangerous
        ram_outdir_Path.mkdir()            # could raise exception?
        output_Path = Path(Path(target).name.replace('.', '_') + "_sentiment-textblob.txt")
        ram_output_Path = ram_outdir_Path / output_Path
        
        print ("running textblob_main.run...", flush=True)
        (sentiment, polarity) = textblob_main.run (ram_target_Path, ram_output_Path)


        #
        # Save output files:
        #
        self.s3filestore.upload_dir_content (str (ram_outdir_Path), S3_OUTPUT_DIR)

        #
        # Populate the database
        #
        # Read the text and metadata back..
        full_text = ram_target_Path.read_text()
        
        # populate the record into the table:
        row = dict()
        row['path'] = s3_file_path
        row['full_text'] = full_text
        row['sentiment'] = sentiment # 'neutral', 'positive', or 'negative'
        row['polarity'] = polarity # <0 = negative, 0 = neutral, > 0 = positive
        row['service_name'] = SERVICE_NAME
        
        #
        # Clean ups and return database row
        #
        self.cleanup (ram_target_Path)
        self.cleanup (ram_outdir_Path)
        return row
        
    def cleanup(self, fileOrDir_Path):
        print (f"cleaning up..", flush=True)
        if fileOrDir_Path.is_file():
            fileOrDir_Path.unlink()
            
        if fileOrDir_Path.is_dir():
            shutil.rmtree (fileOrDir_Path) # dangerous
