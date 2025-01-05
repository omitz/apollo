import unittest

from tesseract_main import run_image_to_data


class TestTesseractMain(unittest.TestCase):

    def test_run_image_to_data_arabic(self):
        in_image_path_string = 'tests/images/arabic_picture_1.tif'
        image, results = run_image_to_data(in_image_path_string)
        self.assertIn('لصناعة', results['text'])

    def test_run_image_to_data_french(self):
        in_image_path_string = 'tests/images/french_picture_1.png'
        image, results = run_image_to_data(in_image_path_string)
        self.assertIn('écarté', results['text'])

    def test_run_image_to_data_russian(self):
        in_image_path_string = 'tests/images/russian_picture_1.png'
        image, results = run_image_to_data(in_image_path_string)
        self.assertIn('Может', results['text'])