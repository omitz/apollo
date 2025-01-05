import argparse, os
from boto.s3.connection import S3Connection, Bucket, Key

from obj_det.od_file_utils import make_outfile_name, get_s3_outdir


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', help="File who's output should be in S3.")
    return parser.parse_args()


def main(args):
    outfile = make_outfile_name(args.input)
    s3_outpath = os.path.join(get_s3_outdir(), outfile)
    # create connection
    conn = S3Connection(os.getenv('AWS_ACCESS_KEY_ID'), os.getenv('AWS_SECRET_ACCESS_KEY'))
    b = Bucket(conn, os.environ.get('BUCKET_NAME'))
    k = Key(b)
    k.key = s3_outpath
    key_inst = b.get_key(k)
    if key_inst is None:
        raise Exception('The expected output file does not exist in the S3 bucket.')
    else:
        print('Test passed.')


if __name__ == '__main__':
	args = parse_args()
	main(args)