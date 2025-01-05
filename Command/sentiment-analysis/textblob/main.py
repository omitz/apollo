#!/usr/bin/env python3
#

import argparse
import os

from textblob_rabbit_consumer import TextBlobRabbitConsumer
from textblob_analytic import TextBlobAnalytic, SERVICE_NAME


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
    analytic = TextBlobAnalytic (SERVICE_NAME)
    rabbit_consumer = TextBlobRabbitConsumer (SERVICE_NAME, 'ApolloExchange', analytic)
    rabbit_consumer.run ()
    pass
