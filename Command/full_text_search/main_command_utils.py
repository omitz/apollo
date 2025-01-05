#!/usr/bin/env python3
"""
APOLLO full-text search module.

Currently, this module uses the built-in text search feature from
Postgres.  The input is just an arbitrary text file.  Later we will
handle other document file formats like MS word, pdf, etc.

The text file is also fed to NER queue for further processing.

2020-07-20

"""
import sys
import os
from pathlib import Path
import argparse

from commandutils import postgres_utils, models
from commandutils import rabbit_utils, s3_utils
from commandutils.rabbit_worker import RabbitWorker
from commandutils.ApolloMessage import ApolloMessage

from sqlalchemy import func

S3_OUTPUT_DIR = 'outputs/full_text_search'
SERVICE_NAME = 'full_text_search'

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
        A s3 path.  eg.,"s3://apollo-source-data/input/audio/bill_gates-TED.mp3".

    Returns:
      None
    """
    #
    # Access S3 bucket and save file to ram disk:
    #
    (s3, target) = s3_utils.access_bucket_and_download (s3_file_path, "/dev/shm")
    ram_target_Path = Path("/dev/shm") / Path(target).name

    #
    # Populate the database
    #
    postgres_utils.init_database (models.SearchFullText)
    engine = postgres_utils.get_engine()
    session = postgres_utils.get_session(engine)
    processed = postgres_utils.check_processed (s3_file_path, session, models.SearchFullText)

    if not processed:  # If this data source isn't in the postgres db yet
        # Read the text
        full_text = ram_target_Path.read_text()

        # populate the record into the table:
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
        
        postgres_utils.save_record_to_database (engine, row, models.SearchFullText)
        print ("saved to database", flush=True)
    else:
        print ("{s3_file_path} already saved to database", flush=True)


    #
    # Also feed to NER
    #
    print("Sending message to named entity recognition", flush=True)
    rabbit_utils.post_message_to_queue('named_entity_recognition', s3_file_path)


    #
    # Clean ups
    #
    print (f"cleaning up..", flush=True)
    if ram_target_Path.is_file():
        ram_target_Path.unlink()

    print (f"done processing {s3_file_path}!", flush=True)

    
if __name__ == '__main__':

    args = parse_args()

    bunny = RabbitWorker('ApolloExchange', SERVICE_NAME)
    bunny.service_fullname = 'Full-text Search'
    bunny.work (process_file)
