import os
from unittest import TestCase, skipIf
from unittest.mock import MagicMock

import numpy as np
import cv2

from face.facenet_video_rabbit_consumer.analytic import FacenetVideoAnalytic, SortFace


class TestAnalytic(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.mock_filestore = MagicMock()
        cls.test_filepath = 'face/facenet_video_rabbit_consumer/tests/test_files/red.mp4'

    def test_detect_and_track(self):
        # Test that detect_and_track returns an empty dictionary when there are no faces in the video
        self.analytic = FacenetVideoAnalytic('face_vid', self.mock_filestore, download_recog_model=False)
        input_video = cv2.VideoCapture(self.test_filepath)
        unique_detections = self.analytic.detect_and_track(input_video)
        self.assertFalse(unique_detections)

    def test_update_unique_detections(self):
        # Test that the unique_detections dict gets updated correctly
        self.analytic = FacenetVideoAnalytic('face_vid', self.mock_filestore, download_recog_model=False)
        random_face = np.zeros((100, 100, 3))
        other_random_face = np.zeros((100, 100, 3))
        unique_detections = {'ids': [2.0, 3.0],
                             'det_conf': [0.9999319314956665, 0.9998565912246704],
                             'face_arr': [random_face, other_random_face],
                             'frames': [[0], [0]]}
        frame = np.ones((200, 200, 3))
        sortface = SortFace(np.array([0, 1, 2, 3, 3.0]), frame)
        confidence = 0.99999999
        video_frames_count = 60
        self.analytic.update_unique_detections(sortface, confidence, unique_detections, video_frames_count)
        expected = {'ids': [2.0, 3.0],
                    'det_conf': [0.9999319314956665, confidence],
                    'frames': [[0], [0, 60]]}
        for k, v in expected.items():
            self.assertListEqual(unique_detections[k], expected[k])
        # Check that the face array was updated as well
        face_max = np.amax(unique_detections['face_arr'][1])
        self.assertEqual(face_max, 1)

    def test_detect_and_recog_no_faces(self):
        self.analytic = FacenetVideoAnalytic('face_vid', self.mock_filestore, download_recog_model=False)
        results_dict = self.analytic.detect_and_recog(self.test_filepath)
        self.assertFalse(results_dict)

    @skipIf(os.getenv('JENKINS'), "Test does not work in jenkins bc the recognition model file is stored S3.")
    def test_detect_and_recog(self):
        # Test that a known person is detected and recognized
        self.analytic = FacenetVideoAnalytic('face_vid')
        results_dict = self.analytic.detect_and_recog('face/facenet_video_rabbit_consumer/tests/test_files/leo_ellen.mp4')
        self.assertIn('Leonardo_DiCaprio', results_dict['prediction'])

