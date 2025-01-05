#!/usr/bin/env python3
"""APOLLO speech-to-text module.

This module is responsible for transcribing audio files. The output
text is then subseqently fed to NER.

2020-08-05: converted to Apollo package.
"""

import argparse
import os

from apollo import S3FileStore
from vosk_rabbit_consumer import VoskRabbitConsumer
from vosk_analytic import VoskAnalytic, SERVICE_NAME


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
        s3filestore.download_dir('local/speech-to-text/vosk/vosk-model-en-us-aspire-0.2', 'model')

    analytic = VoskAnalytic (SERVICE_NAME)
    rabbit_consumer = VoskRabbitConsumer (SERVICE_NAME, 'ApolloExchange', analytic)
    rabbit_consumer.run ()
