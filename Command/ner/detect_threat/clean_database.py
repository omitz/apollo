import pandas as pd
from nltk.stem.snowball import SnowballStemmer

violent_words_og = "violent_words.txt"

words_df = pd.read_csv(violent_words_og, sep='\t', header=None)
words_df.columns = ['letter', 'words']
print(words_df.head())

words_chunks = words_df.words.values
words_list = []
for chunk in words_chunks:
    words = chunk.split(',')
    for word in words:
        words_list.append(word)

stems = []
stemmer = SnowballStemmer(language='english')
for word in words_list:
    stems.append(stemmer.stem(word))

with open('cleaned_violent_words.txt', 'w') as f:
    for word in stems:
        f.write(f'{word.strip()},')
pass