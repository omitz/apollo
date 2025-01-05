import cv2
import os, glob
import pandas as pd
import numpy as np

'''
https://stackoverflow.com/questions/57207975/what-is-an-efficient-way-to-crop-out-a-slanted-box-from-image
'''

def postprocess(results_dir):
    # If not words dir in result, make words dir
    words_dir = os.path.join(results_dir, 'word_imgs')
    if not os.path.exists(words_dir):
        os.makedirs(words_dir)

    for file in os.listdir(results_dir):
    # For each text file,
        if file.endswith('.txt'):
            print(f'Processing {file}')
            path = os.path.join(results_dir, file)
            df = pd.read_csv(path, sep=',', header=None)
            # Make any negatives 0
            df[df < 0] = 0

            df.columns = ['ulx', 'uly', 'urx', 'ury', 'lrx', 'lry', 'llx', 'lly'] # upper-left x, upper-left y, etc.
            img_name, ext = os.path.splitext(file)
            # Don't use the img output from inference bc it has red boxes drawn on it
            img_name = img_name[4:]
            # load the image
            img_path = os.path.join(results_dir, 'test_imgs', img_name)
            matching_paths = glob.glob(f'{img_path}*', recursive=False)
            img_path = matching_paths[0]
            img = cv2.imread(img_path)

            # For each row in txt
            for index, row in df.iterrows():
                # get the width and height
                width_start = min(row.ulx, row.llx)
                width_end = max(row.urx, row.lrx)
                width = width_end - width_start

                height_start = min(row.uly, row.ury)
                height_end = min(row.lly, row.lry)
                height = height_end - height_start

                # Format slanted rectangle coords as an array with shape (4, 2)
                original = np.float32([[row.ulx, row.uly],
                                     [row.urx, row.ury],
                                     [row.lrx, row.lry],
                                     [row.llx, row.lly]])
                # Define corresponding points in output image
                output = np.float32([[0, 0],
                                   [width, 0],
                                   [width, height],
                                   [0, height]])

                # Get perspective transform and apply it
                transform = cv2.getPerspectiveTransform(original, output)
                word_img = cv2.warpPerspective(img, transform, (width, height))

                # save img to words dir
                word_name = f'{img_name}_{index}.png'
                word_path = os.path.join(words_dir, word_name)
                cv2.imwrite(word_path, word_img)
            print(f'finished: {file}\n')