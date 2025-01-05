from FaceData import *
import pandas as pd
import shutil

data = FaceData('/home/HQ/lneff/apollo/face/face/vggface2_test/test')


def save_csv():
    # Save out a csv with all of the filepaths
    # Save out known train, known test, and pretend unknown (also for testing)
    data_dict = {'known_train': data.known_train_files, 'known_test': data.known_test_files, 'unknown_test': data.unknown_files}
    df = pd.DataFrame(dict([(k, pd.Series(v)) for k, v in data_dict.items()]))
    df.to_csv('../train_test_split.csv', index=False)


def copy_images_known(filelist, outdir):
    '''
    Copy the train images to a dir
    '''
    if not os.path.exists(outdir):
        os.mkdir(outdir)
    # Copy the train files to a new location for facenet
    for file in filelist:
        els = file.split('/')
        person = els[-2]
        person_dir = os.path.join(outdir, person)
        if not os.path.exists(person_dir):
            os.mkdir(person_dir)
        dest = os.path.join(person_dir, els[-1])
        shutil.copyfile(file, dest)


def copy_images_unknown(filelist, outdir):
    if not os.path.exists(outdir):
        os.mkdir(outdir)
    unknown_dir = os.path.join(outdir, 'Unknown')
    if not os.path.exists(unknown_dir):
        os.mkdir(unknown_dir)
    # Copy the train files to a new location for facenet
    for file in filelist:
        els = file.split('/')
        person = els[-2]
        filename = f'{person}_{els[-1]}'
        dest = os.path.join(unknown_dir, filename)
        shutil.copyfile(file, dest)


def main():
    save_csv()
    copy_images_known(data.known_train_files, '/home/HQ/lneff/apollo/tmp/face_train/')
    copy_images_known(data.known_test_files, '/home/HQ/lneff/apollo/tmp/face_test/')
    copy_images_unknown(data.unknown_files, '/home/HQ/lneff/apollo/tmp/face_test/')


if __name__ == '__main__':
    main()
