import pyclamd
import tempfile
import shutil
import pathlib
import argparse
import logging
import os

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
ch = logging.StreamHandler()
ch.setLevel(logging.DEBUG)
logger.addHandler(ch)


class Scanner(object):
    def __init__(self):
        self.scanner = pyclamd.ClamdAgnostic()

    def _copy_file(self, filepath):
        _, temppath = tempfile.mkstemp()
        logger.debug(temppath)
        shutil.copy2(filepath, temppath)
        return temppath

    def scan_file(self, filepath: str) -> str:
        temppath = self._copy_file(filepath)
        results = self.scanner.scan_file(temppath)
        pathlib.Path(temppath).unlink()
        return results

    def is_clean(self, filepath: str) -> bool:
        temppath = self._copy_file(filepath)
        results = self.scanner.scan_file(temppath) is None
        pathlib.Path(temppath).unlink()
        return results


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', help="File to scan")
    return parser.parse_args()


def write_to_file(filename, content):
    f = open(filename, "w")
    f.write(content)
    f.close()


def get_output_filename(input_filename):
    (path, extname) = os.path.splitext(input_filename)
    (_, filename) = os.path.split(path)
    return os.path.join(filename + extname.replace(".","_") + "_virus_scan.txt")


def scan(filename):
    scanner = Scanner()
    return scanner.is_clean(filename)


def main():
    args = parse_args()
    result = scan(args.input)
    output_filename = get_output_filename(args.input)
    write_to_file(output_filename, str(result))


if __name__ == '__main__':
    main()
