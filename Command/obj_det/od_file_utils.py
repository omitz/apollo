import os


def make_outfile_name(filename):
    basename = os.path.basename(filename)
    filename, _ = os.path.splitext(basename)
    outname = f'{filename}_results.json'
    return outname


def get_s3_outdir():
    return 'outputs/obj_det'