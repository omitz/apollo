from unittest import TestCase
import warnings

from ner_spacy.ner_utils import load_and_clean_doc, concat_text
from commandutils.neo4j_utils import wait_for_neo4j

class TestNerUtils(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.sample = 'In my opinion depending on discussions with the IMF and so on'
        wait_for_neo4j()

    def test_load_and_clean_doc_text(self):
        text = load_and_clean_doc('ner_spacy/tests/test_files/test space.txt', 'text/plain')
        self.assertIn(self.sample, text)

    def test_load_and_clean_doc_doc(self):
        text = load_and_clean_doc('ner_spacy/tests/test_files/test_doc space.doc', 'application/msword')
        self.assertIn(self.sample, text)

    def test_load_and_clean_doc_docx(self):
        text = load_and_clean_doc('ner_spacy/tests/test_files/test_docx space.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')
        self.assertIn(self.sample, text)

    def test_load_and_clean_doc_pdf(self):
        text = load_and_clean_doc('ner_spacy/tests/test_files/test_pdf space.pdf', 'application/pdf')
        self.assertIn(self.sample, text)

    def test_load_and_clean_doc_unknown(self):
        with warnings.catch_warnings(record=True) as w:
            text = load_and_clean_doc('ner_spacy/tests/test_files/test.txt', 'some_new_filetype')
            assert len(w) > 0

    def test_concat_text(self):
        doc_tokens = ['This', 'is', 'a', 'sentence', 'with', 'someone', ',', 'named', 'Joe', ',', 'who', 'is', 'a', 'VIP', '.']
        indices_to_return = [7, 8, 9]
        snippets = concat_text(doc_tokens, indices_to_return)
        self.assertEqual('... named Joe,...', snippets)

    def test_concat_text_end_of_text(self):
        doc_tokens = ['This', 'is', 'a', 'sentence', 'with', 'someone', ',', 'named', 'Joe', ',', 'who', 'is', 'a', 'VIP', '.']
        indices_to_return = [2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
        snippets = concat_text(doc_tokens, indices_to_return)
        self.assertEqual('... a sentence with someone, named Joe, who is a VIP.', snippets)
