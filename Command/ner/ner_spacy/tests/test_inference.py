from unittest import TestCase

from ner_spacy.inference import inference
from commandutils.neo4j_utils import wait_for_neo4j

class TestInference(TestCase):

    @classmethod
    def setUpClass(cls):
        wait_for_neo4j()

    def test_inference_no_text(self):
        # Test that inference can handle an empty txt file
        local_filepath = 'ner_spacy/tests/test_files/test_empty.txt'
        df = inference(local_filepath, 'text/plain')
        self.assertEqual(len(df.index), 0)
