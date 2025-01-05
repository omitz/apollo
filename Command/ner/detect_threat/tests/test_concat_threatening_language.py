from unittest import TestCase
import spacy
import nltk
import pandas as pd
from ner_spacy.ner_utils import load_and_clean_doc
from detect_threat.detect_threat import DocData, concat_threatening_language

class TestConcat_threatening_language(TestCase):

    @classmethod
    def setUpClass(cls):
        nltk.download('punkt')
        cls.nlp = spacy.load('en_core_web_sm')

    def test_concat_threatening_language_test(self):
        text = load_and_clean_doc('ner_spacy/tests/test_files/test.txt', 'text/plain')
        doc = self.nlp(text)
        df = pd.read_csv('detect_threat/cleaned_violent_words.txt', sep=',', header=None)
        threat_words = list(df.loc[[0]].values[0])
        snippets = concat_threatening_language(DocData(doc), threat_words, 'Hassan Nasrallah')
        key_phrases = ['The powerful Hezbollah', 'the crisis in', 'its position on']
        for phrase in key_phrases:
            self.assertIn(phrase, snippets)

    def test_concat_threatening_language_punctuation(self):
        text = load_and_clean_doc('ner_spacy/tests/test_files/test_punctuation.txt', 'text/plain')
        doc = self.nlp(text)
        df = pd.read_csv('detect_threat/cleaned_violent_words.txt', sep=',', header=None)
        threat_words = list(df.loc[[0]].values[0])
        snippets = concat_threatening_language(DocData(doc), threat_words, 'Hassan Nasrallah')
        key_phrases = ['The powerful Hezbollah', 'the crisis in', 'its position on']
        for phrase in key_phrases:
            self.assertIn(phrase, snippets)

    def test_concat_threatening_language_no_threat(self):
        text = load_and_clean_doc('ner_spacy/tests/test_files/test2.txt', 'text/plain')
        doc = self.nlp(text)
        df = pd.read_csv('detect_threat/cleaned_violent_words.txt', sep=',', header=None)
        threat_words = list(df.loc[[0]].values[0])
        snippets = concat_threatening_language(DocData(doc), threat_words, 'Hassan Nasrallah')
        print(f'snippets if no threat: {snippets}', flush=True)
        self.assertEqual('', snippets)



