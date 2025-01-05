import sys, os
import argparse
import pandas as pd
import country_converter as co_converter
from langdetect import detect as ld
from langdetect import lang_detect_exception

from detect_threat.detect_threat import detect_threat
from ner_spacy.ner_utils import load_and_clean_doc, load_with_lang_model


def main(args):
    inference(args.file, args.mimetype, args.output)

def inference(text_file, mimetype):
    '''
    :param text_file: The local path to the file, eg /dev/shm/lebanons.txt
    :param mimetype: The mimetype, eg text/plain
    '''
    text = load_and_clean_doc(text_file, mimetype)

    try:
        lang = ld(text) # eg 'en'
        import time
        time.sleep(5)
        print(f'Detected language: {lang}', flush=True)
    except lang_detect_exception.LangDetectException as e:
        print(f'e: {e}', flush=True)
        print('Defaulting to English', flush=True)
        lang = 'en'

    nlp = load_with_lang_model(lang)

    doc = nlp(text)
    print(f'input file:\n{doc}\n')
    results = {'text': [], 'label': [], 'is_vip': [], 'snippets': []}

    # Find named entities
    for entity in doc.ents: # TODO (low priority) spaCy sometimes extracts just a last name as an entity (eg Nasrallah)
        is_vip = False
        snippets = None

        # Skip conditions
        if 'http' in entity.text:
            continue
        label_in_graph = entity.label_
        if entity.label_ in ['WORK_OF_ART', 'LAW', 'LANGUAGE', 'DATE', 'TIME', 'PERCENT', 'QUANTITY', 'ORDINAL', 'CARDINAL']: # See https://spacy.io/api/annotation#named-entities for descriptions of the spacy NER labels
            continue

        elif entity.label_ in ['PERSON', 'PER']: # Some spaCy models use PERSON for people, others use PER
            label_in_graph = 'PERSON'
            # See if this name is an alias for another name
            name_in_text = entity.text
            aliases_df = pd.read_csv('ner_spacy/vip_aliases.csv')
            name_match_df = aliases_df[aliases_df.eq(name_in_text).any(1)]
            if len(name_match_df.index) > 0: # ie If we found this alias in our aliases csv
                is_vip = True
                name_in_graph = name_match_df.name.values[0]
                if lang == 'en':
                    # Run detect threat
                    snippets = detect_threat(doc, name_in_graph)
            else:
                name_in_graph = name_in_text
        elif entity.label_ in ['GPE', 'LOC']: # Some spaCy models use GPE for cities, countries, etc, others use LOC. We'll label all cities, countries, bodies of water, etc as LOC.
            label_in_graph = 'LOC'
            converter_result = co_converter.convert(names=[entity.text], to='name_short') # TODO (low priority) Package converts Dem. People's Rep. of Korea -> South Korea instead of North Korea
            if converter_result != 'not found':
                name_in_graph = converter_result
            else:
                name_in_graph = entity.text
        else:
            name_in_graph = entity.text

        results['text'].append(name_in_graph)
        results['label'].append(label_in_graph)
        results['is_vip'].append(is_vip)
        results['snippets'].append(snippets)

    df = pd.DataFrame.from_dict(results)
    df.drop_duplicates(inplace=True)

    # Save results
    print(f'results:\n{df.head()}', flush=True)

    return df
    


def parse_arguments(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument('-f', '--file', type=str, default='../test_files/test.txt',
                        help='txt file')
    parser.add_argument('-o', '--output', default='.',
                        help="Directory where result dataframe will be written out")
    parser.add_argument('-s', '--source',  required=True,
                        help="s3 file input")
    parser.add_argument('-m', '--mimetype', required=True,
                        help='The mime type (as determined by the filetype_checker. One of '
                             'text/plain, application/msword, application/octet-stream, or application/pdf.')
    return parser.parse_args(argv)


if __name__ == '__main__':
    args = parse_arguments(sys.argv[1:])
    main(args)