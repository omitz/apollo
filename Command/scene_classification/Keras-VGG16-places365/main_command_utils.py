#!/usr/bin/env python3
"""
APOLLO scene classificatio module.

This module is responsible for classify images.

2020-07-25
"""

import sys
import os
from pathlib import Path
import shutil
import argparse
import vgg16Places365_main

from commandutils import postgres_utils, models
from commandutils import rabbit_utils, s3_utils
from commandutils.rabbit_worker import RabbitWorker

S3_OUTPUT_DIR = 'outputs/scene_classification/'


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
    """
    
    #
    # Access S3 bucket and save file to ram disk:
    #
    (s3, target) = s3_utils.access_bucket_and_download (s3_file_path, "/dev/shm")
    ram_target_Path = Path("/dev/shm") / Path(target).name

    #
    # Classify the image file:
    #
    ram_outdir_Path = Path("/dev/shm") / str(os.getpid())
    if ram_outdir_Path.is_dir():
        shutil.rmtree(ram_outdir_Path) # dangerous
    ram_outdir_Path.mkdir()            # could raise exception?
    output_Path = Path(Path(target).name.replace('.', '_') + "_scene-places365.txt")
    ram_output_Path = ram_outdir_Path / output_Path
    print ("running vgg16Places365_main.run...", flush=True)
    (classHier, topClassesIdxs) = vgg16Places365_main.run (ram_target_Path, ram_output_Path)
    if classHier == None:
        print ("ERROR: process_file Failed!", flush=True)
    else:
        # Save result (both text and image overlay) back to S3
        s3_utils.save_results_to_s3 (ram_outdir_Path, s3, S3_OUTPUT_DIR)

        # Populate the database:
        postgres_utils.init_database (models.ClassifyScene)
        engine = postgres_utils.get_engine()
        session = postgres_utils.get_session(engine)
        processed = postgres_utils.check_processed (s3_file_path, session, models.ClassifyScene)

        if not processed:  # If this image isn't in the postgres db yet
            row = dict()
            row['path'] = s3_file_path
            row['class_hierarchy'] = classHier
            row['top_five_classes'] = [int(idx) for idx in topClassesIdxs] # sql does not like numpy
            postgres_utils.save_record_to_database(engine, row, models.ClassifyScene)
            print ("saved to database", flush=True)
        else:
            print ("{s3_file_path} already saved to database", flush=True)
    #
    # clean up
    #
    print("cleaning up", flush=True)
    if ram_target_Path.is_file():
        ram_target_Path.unlink()
    if os.path.isdir(ram_outdir_Path):
        shutil.rmtree(ram_outdir_Path) # dangerous

    print (f"done processing {s3_file_path}!", flush=True)


if __name__ == '__main__':

    args = parse_args()

    if args.debug:
        vgg16Places365_main.run ("6.jpg", "/dev/stdout")
        sys.exit (0)

    
    bunny = RabbitWorker('ApolloExchange', 'scene_places365')
    bunny.service_fullname = 'Scene Classification'
    bunny.work (process_file)
