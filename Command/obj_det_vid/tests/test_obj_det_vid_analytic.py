import unittest
from unittest import TestCase
import os
import warnings
from obj_det_vid_analytic import ObjDetVidAnalytic


class TestObjDetVidAnalytic(TestCase):

    @classmethod
    def setUpClass(cls):
        '''
        Initialize the Analytic.
        The testing_in_jenkins parameter allows us to instantiate the analytic without downloading the model from S3. This way, if additional abstract methods are implemented in apollo's Analytic (and not yet implemented in ObjDetVidAnalytic, this setUpClass function will catch that.
        '''
        if os.getenv('JENKINS'):
            cls.analytic = ObjDetVidAnalytic('object_detection_vid', testing_in_jenkins=True)
        else:
            cls.analytic = ObjDetVidAnalytic('object_detection_vid')

    def test_file_check(self):
        '''
        Test that file_check raises an error when passed a file with 0 bytes.
        '''
        with self.assertRaises(ValueError):
            path = 'videos/inputs/empty.mp4'
            self.analytic.file_check(path)

    def test_get_rotation(self):
        '''
        Test that get rotation triggers a warning.
        '''
        with self.assertWarns(Warning):
            path = 'nonexistent_bad_input'
            self.analytic.get_rotation(path)

    # Test that rotated videos are handled correctly. (The network is not rotation-invariant, ie if frames are processed without being oriented correctly, the detections will be incorrect.) The rot*mp4 videos all have similar content (cars) and were taken with 4 different orientations.
    @unittest.skipIf(os.getenv('JENKINS'),
                     "Test does not work in jenkins because the model file needs to be downloaded from S3.")
    def test_detect_rot90(self):
        path = 'videos/inputs/rot90.mp4'
        _, objects_and_seconds = self.analytic.detect(path)
        car_seconds = {1, 2, 3, 4}
        self.assertTrue(car_seconds.issubset(objects_and_seconds['car']['seconds']))

    @unittest.skipIf(os.getenv('JENKINS'),
                     "Test does not work in jenkins because the model file needs to be downloaded from S3.")
    def test_detect_rot180(self):
        path = 'videos/inputs/rot180.mp4'
        _, objects_and_seconds = self.analytic.detect(path)
        car_seconds = {1, 2, 3, 4}
        self.assertTrue(car_seconds.issubset(objects_and_seconds['car']['seconds']))

    @unittest.skipIf(os.getenv('JENKINS'),
                     "Test does not work in jenkins because the model file needs to be downloaded from S3.")
    def test_detect_rot270(self):
        path = 'videos/inputs/rot270.mp4'
        _, objects_and_seconds = self.analytic.detect(path)
        car_seconds = {1, 2, 3, 4}
        self.assertTrue(car_seconds.issubset(objects_and_seconds['car']['seconds']))

    @unittest.skipIf(os.getenv('JENKINS'),
                     "Test does not work in jenkins because the model file needs to be downloaded from S3.")
    def test_detect_rotNone(self):
        path = 'videos/inputs/rotNone.mp4'
        _, objects_and_seconds = self.analytic.detect(path)
        car_seconds = {1, 2, 3}
        self.assertTrue(car_seconds.issubset(objects_and_seconds['car']['seconds']))

    @unittest.skipIf(os.getenv('JENKINS'),
                     "Test does not work in jenkins because the model file needs to be downloaded from S3.")
    def test_detectObjectsFromVideo_no_res(self):
        '''
        Test a video where there shouldn't be any detections.
        '''
        path = 'videos/inputs/red_clip.mp4'
        self.analytic.detector.detectObjectsFromVideo(input_file_path=path,
                                                                frame_detection_interval=4,
                                                                log_progress=False,
                                                                save_detected_video=False)
        self.intermediate_output = self.analytic.detector.intermediate_output
        output_frames_dict = self.analytic.load_all_frame_detections()
        objects_and_seconds = self.analytic.objects_by_second(path, output_frames_dict)
        # output_frames_dict should be empty and thus should evaluate to False
        self.assertFalse(objects_and_seconds)

    @unittest.skipIf(os.getenv('JENKINS'),
                     "Test does not work in jenkins because the model file needs to be downloaded from S3.")
    def test_detectObjectsFromVideo_multi_res(self):
        '''
        Test a video where there should be multiple detections.
        '''
        path = 'videos/inputs/holo_clip.mp4'
        self.analytic.detector.detectObjectsFromVideo(input_file_path=path,
                                                                frame_detection_interval=4,
                                                                log_progress=False,
                                                                save_detected_video=False)
        self.analytic.intermediate_output = self.analytic.detector.intermediate_output
        output_frames_dict = self.analytic.load_all_frame_detections()
        objects_and_seconds = self.analytic.objects_by_second(path, output_frames_dict)
        self.assertIn('book', objects_and_seconds)
        self.assertIn('person', objects_and_seconds)

    @unittest.skipIf(os.getenv('JENKINS'),
                     "Test does not work in jenkins because the model file needs to be downloaded from S3.")
    def test_detectObjectsFromVideo_filetypes(self):
        '''
        Test that that detectObjectsFromVideo can process avi, mkv, mov, mp4, and webm. (Note: It's not expected that each file should have the same result.)
        '''
        for path in ['videos/inputs/holo_clip.avi',
                     'videos/inputs/holo_clip.mkv',
                     'videos/inputs/holo_clip.mov',
                     'videos/inputs/holo_clip.mp4',
                     'videos/inputs/holo_clip.webm']:
            self.analytic.detector.detectObjectsFromVideo(input_file_path=path,
                                                          frame_detection_interval=50,
                                                          log_progress=False,
                                                          save_detected_video=False)
            self.analytic.intermediate_output = self.analytic.detector.intermediate_output
            output_frames_dict = self.analytic.load_all_frame_detections()
            objects_and_seconds = self.analytic.objects_by_second(path, output_frames_dict)
            custom_failure_msg = f'\n\nAssertion failed for file {path}.'
            self.assertIn('book', objects_and_seconds, custom_failure_msg)