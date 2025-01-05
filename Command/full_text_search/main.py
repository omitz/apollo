#!/usr/bin/env python3
"""APOLLO full-text search module.

Currently, this module uses the built-in text search feature from
Postgres.  The input is just an arbitrary text file.  Later we will
handle other document file formats like MS word, pdf, etc.

The text file is also fed to NER queue for further processing.

The version uses the Apollo package framework.
2020-08-05
"""

import argparse
from full_text_search_rabbit_consumer import FullTextSearchRabbitConsumer
from full_text_search_analytic import FullTextSearchAnalytic, SERVICE_NAME


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

    analytic = FullTextSearchAnalytic (SERVICE_NAME)
    rabbit_consumer = FullTextSearchRabbitConsumer (SERVICE_NAME, 'ApolloExchange', analytic)
    rabbit_consumer.run ()
