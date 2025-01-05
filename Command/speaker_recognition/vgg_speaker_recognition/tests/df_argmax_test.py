import pandas as pd
import numpy as np

'''
This script just provides a small example helpful for understand np's argmax and pandas's iloc.
'''

data = {'person': ['Mohammad_Ali1', 'Mohammad_Ali2', 'Mohammad_Ali3'], 'probability': [0.62476075, 0.64629614, 0.621468], 'wav': ['/tmp/speaker_recog/audio_files/Mohammad_Ali/00306.wav', '/tmp/speaker_recog/audio_files/Mohammad_Ali/00114.wav', '/tmp/speaker_recog/audio_files/Mohammad_Ali/00490.wav']}

df = pd.DataFrame.from_dict(data)

print(df.head())
print("\n")

best_match_index = np.argmax(np.array(df.probability.values))
df = df.iloc[[best_match_index]]
print(df.head())

df.to_csv('test.csv', index=False)