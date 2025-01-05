#!/usr/bin/env python3
"""APOLLO Tesseract OCR module.

This module is responsible for extracting text from image files.

2020-08-06: Converted to using Apollo package.
"""

import argparse
from tesseract_rabbit_consumer import TesseractRabbitConsumer
from tesseract_analytic import TesseractAnalytic, SERVICE_NAME


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

    analytic = TesseractAnalytic (SERVICE_NAME)
    rabbit_consumer = TesseractRabbitConsumer (SERVICE_NAME, 'ApolloExchange', analytic)
    rabbit_consumer.run ()
