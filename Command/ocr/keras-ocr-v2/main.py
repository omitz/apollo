#!/usr/bin/env python3
"""APOLLO Keras-OCR module.

This module is responsible for extracting text from image files.

2020-08-31: Download model from S3.
2020-08-06: Converted to using Apollo package.
"""

import argparse
import os
import pathlib

from apollo import S3FileStore
from keras_ocr_rabbit_consumer import KerasOcrRabbitConsumer
from keras_ocr_analytic import KerasOcrAnalytic, SERVICE_NAME


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


if __name__ == '__main__':

    args = parse_args()

    if not os.path.exists ("model"):
        print(f"downloading model files from S3!")
        s3filestore = S3FileStore()
        s3filestore.download_dir('local/ocr/keras-ocr', 'model')

    if not os.path.exists ("/root/.keras-ocr"):
        currentPath = pathlib.Path(os.path.dirname(os.path.realpath(__file__)))
        os.symlink (currentPath / 'model', '/root/.keras-ocr')
        
    analytic = KerasOcrAnalytic (SERVICE_NAME)
    rabbit_consumer = KerasOcrRabbitConsumer (SERVICE_NAME, 'ApolloExchange', analytic)
    rabbit_consumer.run ()
