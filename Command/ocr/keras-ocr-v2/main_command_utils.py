#!/usr/bin/env python3
"""
APOLLO Keras-OCR module.

This module is responsible for extracting text from image files.
2020-07-14
"""

import sys
import os
from pathlib import Path
import shutil
import argparse
import kerasOcr_main

from commandutils import postgres_utils, models
from commandutils import rabbit_utils, s3_utils
from commandutils.rabbit_worker import RabbitWorker
from commandutils.ApolloMessage import ApolloMessage

from sqlalchemy import func

S3_OUTPUT_DIR = 'outputs/ocr'
SERVICE_NAME = 'ocr_keras'

def parse_args() -> argparse.Namespace:
    """Parses commandline argurments.

    Returns:
      An argparse.Namespace object whose member variables correspond
      to commandline agruments. For example the "--debug" commandline
      option becomes member variable .debug.
    """
    parser = argparse.ArgumentParser()
    parser.add_argument('-d', '--debug', required=False, action="store_true")
    args = parser.parse_args()
    return args


def process_file(s3_file_path: str):
    """Processes file and generates analytics.

    Args:
      s3_file_path:
        A s3 path.  eg.,"s3://apollo-source-data/input/ocr/ocr.png".

    Returns:
      None
    """
    #
    # Access S3 bucket and save file to ram disk:
    #
    (s3, target) = s3_utils.access_bucket_and_download (s3_file_path, "/dev/shm")
    ram_target_Path = Path("/dev/shm") / Path(target).name

    #
    # OCR the image file:
    #
    ram_outdir_Path = Path("/dev/shm") / str(os.getpid())
    if ram_outdir_Path.is_dir():
        shutil.rmtree(ram_outdir_Path) # dangerous
    ram_outdir_Path.mkdir()            # could raise exception?
    output_Path = Path(Path(target).name.replace('.', '_') + "_ocr-keras-v2.txt")
    ram_output_Path = ram_outdir_Path / output_Path
    debug_Path = Path(Path(target).name.replace('.', '_') + "_ocr-keras-overlay-v2.png")
    ram_debug_Path = ram_outdir_Path / debug_Path
    metadata_Path = Path(s3_utils.S3_BUCKET) / S3_OUTPUT_DIR / ram_debug_Path.name
    metadata_s3_path = "s3://" +  str (metadata_Path)

    print ("kerasOcr_main.run...", flush=True)
    succeed = kerasOcr_main.run (ram_target_Path, ram_output_Path, ram_debug_Path)

    #
    # Save result (both text and image overlay) back to S3
    #
    fulltext_s3_path = ""
    if not succeed:
        print ("ERROR: process_file Failed!", flush=True)
    else:
        s3_utils.save_results_to_s3 (ram_outdir_Path, s3, S3_OUTPUT_DIR)
        fulltext_Path = Path(s3_utils.S3_BUCKET) / S3_OUTPUT_DIR / ram_output_Path.name
        fulltext_s3_path = "s3://" +  str (fulltext_Path)

        # Populate the database
        postgres_utils.init_database (models.SearchFullText)
        engine = postgres_utils.get_engine()
        session = postgres_utils.get_session(engine)
        processed = postgres_utils.check_processed (s3_file_path, session, models.SearchFullText)

        if not processed:  # If this data source isn't in the postgres db yet
            # Read the text back..
            full_text = ram_output_Path.read_text()

            # populate the record into the table:
            row = dict()
            row['path'] = s3_file_path
            row['fulltext_path'] = fulltext_s3_path
            row['full_text'] = full_text # Not necessary to add to db.  -- TBF

            clean_full_text = full_text.replace("'", "")
            row['search_vector'] = func.to_tsvector('english', clean_full_text)
            row['service_name'] = SERVICE_NAME

            # metadata are service dependent
            row['metadata_path'] = metadata_s3_path
            row['meta_data'] = None # Not necessary to add to db.  -- TBF
            
            postgres_utils.save_record_to_database (engine, row, models.SearchFullText)
            print ("saved to database", flush=True)
        else:
            print (f"{s3_file_path} already saved to database", flush=True)

        # Feed to NER
        print("Sending message to named entity recognition", flush=True)
        rabbit_utils.post_message_to_queue('named_entity_recognition', fulltext_s3_path)

        
    #
    # Clean ups
    #
    print("cleaning up..", flush=True)
    if ram_target_Path.is_file():
        ram_target_Path.unlink()
    if os.path.isdir(ram_outdir_Path):
        shutil.rmtree(ram_outdir_Path) # dangerous

    print (f"done processing {s3_file_path}!", flush=True)
        

if __name__ == '__main__':

    args = parse_args()

    if args.debug:
        kerasOcr_main.run (
            "tests/images/Army_Reserves_Recruitment_Banner_MOD_45156284.jpg",
            "out.txt", "out.png")
        sys.exit (0)
    
    bunny = RabbitWorker('ApolloExchange', SERVICE_NAME)
    bunny.service_fullname = 'Keras OCR'
    bunny.work(process_file)
