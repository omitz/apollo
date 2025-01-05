from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split
import pandas as pd
from commandutils import *
import numpy as np
'''
The intent of this class is to handle all of the data wrangling required to compare models. 
We take the VGGFace2 test set and use it to build train and test sets. These train and test sets are created in such a way that we can evaluate performance on 
A) people who should be classified by name (those the model was trained on) and 
B) people who should be classified as 'Unknown' (people the model was not trained on)
simultaneously.

Split the VGG data we have (VGG test) such that we'll use 50% of the people to test how accurate the system is on people it hasn't seen before.
(The reason for using VGG Test is that it ensures our embeddings are not overfit - as the network used to get the embedding was trained and evaluated on the VGG train and val sets.)
'''


class FaceData:
    def __init__(self, people_dir):
        self.people_dir = people_dir
        self.known = list()
        self.known_train_files = list()
        self.known_test_files = list()
        self.unknown_files = list()

        self.prep_filepaths()

    def prep_filepaths(self):
        person_list = [item for item in os.listdir(self.people_dir) if os.path.isdir(os.path.join(self.people_dir, item))]
        # Set aside 50%
        self.known, pretend_unknowns = train_test_split(person_list, test_size=.5, random_state=1232)
        print(f'self.known: {self.known[:3]}...')

        # Make sure that the short-list VIPs and the demo example person (Lilly Neff) are in the known person dataset
        df = pd.read_csv('/home/HQ/lneff/apollo/apollo/Command/vip/vips.csv')
        vip_shortlist = list(df.Name)
        need_to_be_in_training = vip_shortlist + ['Lilly_Neff'] + ['Hassan_Nasrallah']
        for person in need_to_be_in_training:
            if person in pretend_unknowns:
                pretend_unknowns.remove(person)
                self.known.append(person)

        known_person_files = list_filepaths(self.people_dir, self.known)
        self.unknown_files = list_filepaths(self.people_dir, pretend_unknowns)

        # Only train the SVM (or knn or other classifier) on some of the known person files
        self.known_train_files, self.known_test_files = train_test_split(known_person_files, test_size=.2, random_state=1232)

        # Check that for each known person, they have files in both known_train and known_test
        train_people = [filepath.split('/')[-2] for filepath in self.known_train_files]
        test_people = [filepath.split('/')[-2] for filepath in self.known_test_files]
        train_people = np.unique(train_people).sort()
        test_people = np.unique(test_people).sort()
        assert np.array_equal(train_people, test_people)