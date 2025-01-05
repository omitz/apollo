import argparse, os
from boto.s3.connection import S3Connection, Bucket, Key

from speaker_recognition.vgg_speaker_recognition.src.sr_file_utils import make_outfile_name, get_s3_outdir


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', help="File who's output should be deleted from S3.")
    return parser.parse_args()


def main(args):
    outfile = make_outfile_name(args.input)
    s3_outpath = os.path.join(get_s3_outdir(), outfile)
    # create connection
    conn = S3Connection(os.getenv('AWS_ACCESS_KEY_ID'), os.getenv('AWS_SECRET_ACCESS_KEY'))
    b = Bucket(conn, os.environ.get('BUCKET_NAME'))
    k = Key(b)
    k.key = s3_outpath
    print(f'Deleting {s3_outpath} (if it exists)')
    b.delete_key(k)


if __name__ == '__main__':
	args = parse_args()
	main(args)