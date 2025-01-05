import pandas as pd
import os
from shutil import copyfile

# Cp their images (just first 10) into a new_people directory

# Location of VGG Face 2 test images
IMGS = '/home/HQ/lneff/apollo/face/face/vggface2_test/test'

DF = pd.read_csv('vips.csv')

print(DF.head())


def cp_files(outdir, subset):
    '''
    :param subset: True or False; Whether or not to copy 10 images per person or all images.
    '''
    if not os.path.exists(outdir):
        os.makedirs(outdir)
    for i, row in DF.iterrows():
        vgg_dir = os.path.join(IMGS, row.VGGFace2_ID)
        print(vgg_dir)
        person_dir = os.path.join(outdir, row.Name)
        if not os.path.exists(person_dir):
            os.makedirs(person_dir)
        if subset:
            img_list = os.listdir(vgg_dir)[:10]
        else:
            img_list = os.listdir(vgg_dir)
        for img in img_list:
            src = os.path.join(vgg_dir, img)
            filename = os.path.basename(src)
            dest = os.path.join(person_dir, filename)
            copyfile(src, dest)

# For mobile
cp_files('vips', True)
# For Command
cp_files('vips_large', False)