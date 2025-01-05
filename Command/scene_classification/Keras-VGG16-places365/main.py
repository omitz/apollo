#!/usr/bin/env python3
"""
APOLLO scene classificatio module.

This module is responsible for classify images.

2020-09-09: Download model from S3
2020-08-06: convert to use Apollo package.
"""

import argparse
import os
import pathlib

from apollo import S3FileStore
from places365_rabbit_consumer import Places365RabbitConsumer
from places365_analytic import Places365Analytic, SERVICE_NAME


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
        s3filestore.download_dir ('local/scene_classification/Keras-VGG16-places365', 'model')

    if not os.path.exists ("/root/.keras/models"):
        if not os.path.exists ("/root/.keras"):
            os.mkdir ("/root/.keras")
        currentPath = pathlib.Path(os.path.dirname(os.path.realpath(__file__)))
        os.symlink (currentPath / 'model', '/root/.keras/models')

    analytic = Places365Analytic (SERVICE_NAME)
    rabbit_consumer = Places365RabbitConsumer (SERVICE_NAME, 'ApolloExchange', analytic)
    rabbit_consumer.run ()

