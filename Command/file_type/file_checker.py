import magic
import argparse

from typing import BinaryIO

# TODO
# pull work from MQ
# process message
# extract s3 path
# pull file down
# extract mime type
# write results to data lake


class FileChecker(object):

  def __init__(self):
    self.file_checker = magic.Magic(mime=True)

  def process_file(self, filepath: str) -> str:
    return self.file_checker.from_file(filepath)

  def process_buffer(self, filebytes: BinaryIO) -> str:
    return self.file_checker.from_buffer(filebytes)


def parse_args():
  parser = argparse.ArgumentParser()
  parser.add_argument('-i', '--input', help="File to process")
  return parser.parse_args()


def main():
  args = parse_args()
  file_checker = FileChecker()
  mime_type = file_checker.process_file(args.input)
  print(mime_type)


if __name__ == '__main__':
  main()
