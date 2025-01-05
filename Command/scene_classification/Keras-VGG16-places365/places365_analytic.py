import os
from apollo import Analytic, S3FileStore, PostgresDatabase
from pathlib import Path
import shutil
import mpu.aws as mpuaws
import vgg16Places365_main


S3_OUTPUT_DIR = 'outputs/scene_classification/'
SERVICE_NAME = 'scene_places365'

class Places365Analytic (Analytic):
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
        # Classify the image file:
        #
        ram_outdir_Path = Path(self.ram_storage) / str(os.getpid())
        if ram_outdir_Path.is_dir():
            shutil.rmtree(ram_outdir_Path) # dangerous
        ram_outdir_Path.mkdir()            # could raise exception?
        output_Path = Path(Path(target).name.replace('.', '_') + "_scene-places365.txt")
        ram_output_Path = ram_outdir_Path / output_Path

        print ("running vgg16Places365_main.run...", flush=True)
        (classHier, topClassesIdxs) = vgg16Places365_main.run (ram_target_Path, ram_output_Path)
        
        if classHier == None:
            print ("ERROR: process_file Failed!", flush=True)
            self.cleanup (ram_target_Path)
            self.cleanup (ram_outdir_Path)
            return

        #
        # Save output files:
        #
        self.s3filestore.upload_dir_content (str (ram_outdir_Path), S3_OUTPUT_DIR)

        #
        # Populate the database
        #
        row = dict()
        row['path'] = s3_file_path
        row['class_hierarchy'] = classHier
        row['top_five_classes'] = [int(idx) for idx in topClassesIdxs] # sql does not like numpy
        
        #
        # Also feed to NER
        #
        # TBD..
        
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
        
