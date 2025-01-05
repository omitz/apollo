from unittest import TestCase

import os
import warnings
warnings.filterwarnings('ignore', category=FutureWarning)
import shutil
from face.facenet.src.align.align_dataset_mtcnn import detect
from face.facenet.inference import read_mtcnn_results
from face.facenet.box_utils import rm_overlap


class TestAlignDatasetMTCNN(TestCase):

    def tearDown(self):
        shutil.rmtree(self.mtcnn_results_dir)

    def test_detect(self):

        full_size = '/code/face/facenet/tests/test_images/direct.jpeg'

        self.mtcnn_results_dir = detect(full_size)

        # Read in the mtcnn results txt file as a pandas dataframe
        bbs = os.path.join(self.mtcnn_results_dir, 'bounding_boxes.txt')
        df = read_mtcnn_results(bbs)
        df = rm_overlap(df)

        assert len(df.index) == 3