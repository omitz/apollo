import unittest
import pathlib
from file_checker import FileChecker


class FileCheckerTest(unittest.TestCase):

  @classmethod
  def setUpClass(cls):
    cls.file_checker = FileChecker()

  def test_process_file(self):
    mime = self.file_checker.process_file('file_checker.py')
    self.assertEqual(mime, 'text/x-python')

  def test_from_buffer(self):
    file_contents = pathlib.Path('file_checker.py').read_text()
    mime = self.file_checker.process_buffer(file_contents)
    self.assertEqual(mime, 'text/x-python')
