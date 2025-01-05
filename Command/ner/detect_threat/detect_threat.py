import pandas as pd
from nltk.stem.snowball import SnowballStemmer
from nltk import word_tokenize
from ner_spacy.ner_utils import concat_text

def detect_threat(doc, vip_name_in_graph):
    '''
    Args:
        doc: A spaCy doc
        vip_name_in_graph: The name of the vip
    Convert each word in the text to its grammatical root, then check if any of those roots are in our threatening/violent database (cleaned_violent_words.txt)
    '''
    doc_data = DocData(doc)
    threat_words = load_threat_words()
    snippets = concat_threatening_language(doc_data, threat_words, vip_name_in_graph)
    return snippets


def load_threat_words():
    # Load violent words
    try:
        df = pd.read_csv('cleaned_violent_words.txt', sep=',', header=None)
    except FileNotFoundError:
        # For local, non-docker runs
        df = pd.read_csv('detect_threat/cleaned_violent_words.txt', sep=',', header=None)
    violent_words = list(df.loc[[0]].values[0])
    return violent_words


def concat_threatening_language(doc_data, violent_words, vip_name_in_graph):
    '''
    Args:
        doc_data: DocData object where doc_data.doc.text contains a vip
        violent_words: A list of the grammatical roots of ~500 violent words
        vip_name_in_graph: The VIP's name in graph (ie not one of their aliases)

    Returns: A string of snippets from the text where there is threatening language.
    '''
    # We've made some assumptions about the output of word_tokenize and SnowballStemmer. Here we check those assumptions because if those functions don't preserve every token in the original document, the snippets might be inaccurate.
    if not len(doc_data.stems) == len(doc_data.tokens):
        print('Warning: Snippet might not contain all threatening language.')
    token_indices_to_return = []
    for i, stem in enumerate(doc_data.stems):
        if stem in violent_words:
            # Find that section in the original text
            # Number of words on either side of the threat word
            padding = 10
            if i <= padding: # If we're close to the beginning of the doc
                start = 0
            else:
                start = i - padding
            if (i + padding) >= len(doc_data.tokens): # If we're close to the end of the doc
                end = len(doc_data.tokens)
            else:
                end = i + padding
            for i in range(start, end):
                token_indices_to_return.append(i)
            print(f'Threatening language mentioned with {vip_name_in_graph}: {stem}')
    doc_tokens = doc_data.tokens
    snippets = concat_text(doc_tokens, token_indices_to_return)
    print(f'snippets: {snippets}')
    return snippets


class DocData():
    def __init__(self, doc):
        '''
        Args:
            doc: A spaCy doc
        '''
        self.doc = doc
        # # Tokenize the doc (Separate punctuation, etc.)
        self.tokens = word_tokenize(self.doc.text)
        # Get the grammatical root for each word
        stemmer = SnowballStemmer(language='english')
        self.stems = []
        for token in self.tokens:
            self.stems.append(stemmer.stem(token))
        self.num_chars = len(self.doc.text)