import unittest
from extract_features import extract_feats


class TestExtractFeatures(unittest.TestCase):

    def test_extract_feats(self):
        # Test that the S3 path is put into the img dict
        image_paths = ['tests/all_souls_000011.jpg', 'tests/worcester_000194.jpg']
        source_paths = ['s3/all_souls_000011.jpg', 's3/worcester_000194.jpg']
        imgs_dicts, _ = extract_feats(image_paths, source_paths)
        paths = [k['path'] for k in imgs_dicts]
        self.assertEqual(paths, source_paths)

    def test_extract_feats_no_feats(self):
        # Test that features_found will be False if the image has no DELF features
        image_paths = ['tests/nothing.png']
        imgs_dicts, features_found = extract_feats(image_paths)
        self.assertFalse(features_found)