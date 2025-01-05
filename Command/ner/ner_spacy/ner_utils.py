import textract
import warnings
import spacy
import string


def load_and_clean_doc(filepath, filetype):
    print(f'text file: {filepath}\nfiletype: {filetype}', flush=True)
    if filetype == 'text/plain':
        with open(filepath, 'r') as file:
            text = file.read()
    elif filetype in ['application/msword',
                      'application/vnd.openxmlformats-officedocument.wordprocessingml.document']: # doc and docx
        if filetype == 'application/msword':
            ext = 'doc' # textract uses antiword
        else:
            ext = 'docx' # textract uses python-docx2txt
        try:
            text = textract.process(filepath, extension=ext)
            text = text.decode("utf-8")
        except textract.exceptions.ShellError as e: # TODO This error can be thrown when a very short .doc or .docx created by OpenOffice is processed. For now, we'll assume that a doc with this little text is irrelevant. It'd be better to find an alternate way to read the files (maybe by passing it as an image to ocr-tesseract).
            print(e, flush=True)
            text = ''
    elif filetype == 'application/pdf':
        # from tika import parser as tika_parser
        # text = tika_parser.from_file(text_file)['content'] # Tika works locally, but it would take some extra work to get it running in a docker container
        text = textract.process(filepath, extension='pdf') # textract uses pdftotext
        text = text.decode("utf-8")
    else:
        warnings.warn('Unknown or new filetype. Attempting to read.')
        with open(filepath, 'r') as file:
            text = file.read()
    text = text.replace(u'\uFFFD', '')
    return text


def load_with_lang_model(lang):
    if lang == 'en':
        nlp = spacy.load('en_core_web_sm')  # No Arabic or Russian equivalents available as of 2020-03-19
    elif lang == 'fr':
        nlp = spacy.load('fr_core_news_sm')
    else:
        print('Using multi-language model. Results may vary.', flush=True)
        nlp = spacy.load('xx_ent_wiki_sm')
    return nlp


def concat_text(doc_tokens_or_chars, indices_to_return, spaces=True):
    '''
    Give the tokens or characters (from a document) and the indices of the tokens or characters that should be returned, string together the tokens or characters, string together the text so that omitted text is replaced with an ellipsis.
    Args:
        doc_tokens_or_chars: A list of tokens (ie words, punctuation) or a list of characters (letters, punctuation)
        indices_to_return: A list of indices indicating which tokens (or characters) should be returned
        spaces: Leave as True when using this function to string together words
    '''
    indices_to_return = sorted(set(
        indices_to_return))  # 'set' here removes the overlap which occurs when two threat words are close together
    snippets = ''
    for i in range(len(indices_to_return)):
        # Place ellipsis between non-adjacent tokens
        previous_index_plus_one = indices_to_return[i - 1] + 1
        is_next = indices_to_return[
                      i] == previous_index_plus_one  # if this token came right after the previous token (ie this token is part of the same snippet as the previous token)
        if not is_next:
            snippets += '...'
        word_to_append = doc_tokens_or_chars[indices_to_return[i]]
        if spaces:
            if word_to_append not in string.punctuation:
                snippets += ' '
        snippets += word_to_append
    if len(indices_to_return) > 0 and indices_to_return[-1] != len(
            doc_tokens_or_chars)-1:  # If doesn't include the very end of the document
        snippets += '...'
    return snippets