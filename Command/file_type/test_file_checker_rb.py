import unittest
from file_checker_rb import process_file

class FileCheckerRbTest(unittest.TestCase):

    def test_process_file_text_output_queues(self):
        file = 'test_files/test.txt'
        results = process_file(file)
        results_queues = results['output_queues']
        expected_queues = ['named_entity_recognition', 'full_text_search', 'text_sentiment']
        self.assertListEqual(results_queues, expected_queues)