import nltk
import pandas as pd
import os, sys
import argparse


def main(args):
    nltk.download('punkt')

    # read in the raw text
    filename = args.file
    with open(filename) as f:
        raw = f.read()

    nltk.download('averaged_perceptron_tagger')

    words = nltk.word_tokenize(raw)
    tagged = nltk.pos_tag(words)
    nltk.download('maxent_ne_chunker')
    nltk.download('words')
    named_ent = nltk.ne_chunk(tagged, binary=True)
    results = {'text': [], 'label': []}
    for chunk in named_ent:
        if hasattr(chunk, 'label'):
            if args.verbose:
              for n in chunk:
                print(f'{type(n)}: {n}')
              print(chunk.label(), ' '.join(c[0] for c in chunk))
              print('-----------------')
            text = ' '.join(c[0] for c in chunk)
            results['text'].append(text)
            results['label'].append(chunk.label())

    # Save results
    df = pd.DataFrame.from_dict(results)
    outname = f'{os.path.splitext(os.path.basename(filename))[0]}_result.csv'
    print(f'results:\n{df}')
    df.to_csv(outname, index=False)


def parse_arguments(argv):
    parser = argparse.ArgumentParser()
    parser.add_argument('-f', '--file', type=str, default='../test_files/test.txt',
                        help='txt file')
    parser.add_argument('-v', '--verbose', action='store_true')
    return parser.parse_args(argv)


if __name__ == '__main__':
    main(parse_arguments(sys.argv[1:]))
