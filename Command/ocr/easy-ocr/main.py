#!/usr/bin/env python3
"""APOLLO EasyOCR module.

This module is responsible for extracting text from image files.

2020-09-04: Initial import.
"""

import argparse
import os

from apollo import S3FileStore
from easy_ocr_rabbit_consumer import EasyOcrRabbitConsumer
from easy_ocr_analytic import EasyOcrAnalytic, SERVICE_NAME


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
    analytic = EasyOcrAnalytic (SERVICE_NAME)
    rabbit_consumer = EasyOcrRabbitConsumer (SERVICE_NAME, 'ApolloExchange', analytic)
    rabbit_consumer.run ()
