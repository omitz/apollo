import unittest
import os
import pathlib

from PIL import Image

import ocr

class OcrTest(unittest.TestCase):

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_get_script_languages(self):
        script = 'Han'
        expected_languages = 'eng+chi_sim+chi_tra+kor+jpg'
        languages = ocr.get_script_languages(script)
        self.assertEqual(languages, expected_languages)
        self.assertIsInstance(languages, str)

    def test_get_script_languages_default(self):
        script = 'Klingon' # not supported
        expected_languages = 'eng' # expected default
        languages = ocr.get_script_languages(script)
        self.assertEqual(languages, expected_languages)
        self.assertIsInstance(languages, str)

    def test_detect_latin_script(self):
        current_path = pathlib.Path(os.path.dirname(os.path.realpath(__file__)))
        image_path = current_path / "images" / "ocr.gif"
        img = Image.open(image_path)
        osd = ocr.detect_script_and_orientation(img)
        self.assertEqual(osd['script'], 'Latin')
        img.close()

    def test_detect_han_script(self):
        current_path = pathlib.Path(os.path.dirname(os.path.realpath(__file__)))
        image_path = current_path / "images" / "Chinese.jpg"
        img = Image.open(image_path)
        osd = ocr.detect_script_and_orientation(img)
        self.assertEqual(osd['script'], 'Han')
        img.close()

    def test_detect_arabic_script(self):
        current_path = pathlib.Path(os.path.dirname(os.path.realpath(__file__)))
        image_path = current_path / "images" / "arabic_picture_1.tif"
        img = Image.open(image_path)
        osd = ocr.detect_script_and_orientation(img)
        self.assertEqual(osd['script'], 'Arabic')
        img.close()

    def test_decode(self):
        current_path = pathlib.Path(os.path.dirname(os.path.realpath(__file__)))
        image_path = current_path / "images" / "ocr.gif"
        img = Image.open(image_path)
        osd = ocr.detect_script_and_orientation(img)
        self.assertEqual(osd['script'], 'Latin')
        languages = ocr.get_script_languages(osd['script'])
        ocr_data = ocr.decode(img, languages)
        self.assertTrue(any(o['text'] == "QUICK" for o in ocr_data))
        self.assertTrue(any(o['text'] == "BROWN" for o in ocr_data))
        self.assertTrue(any(o['text'] == "FOX" for o in ocr_data))
        img.close()
