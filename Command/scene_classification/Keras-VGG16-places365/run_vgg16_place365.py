#!/usr/bin/env python3
#
# GPU does (<2GB) does not seem to work??
#

import os
# from urllib.request import urlopen
import numpy as np
import csv
from PIL import Image
from cv2 import resize

from vgg16_places_365 import VGG16_Places365

# TEST_IMAGE_URL = 'http://places2.csail.mit.edu/imgs/demo/6.jpg'
# image = Image.open(urlopen(TEST_IMAGE_URL))
image = Image.open("6.jpg")
image = np.array(image, dtype=np.uint8)
image = resize(image, (224, 224))
image = np.expand_dims(image, 0)

model = VGG16_Places365(weights='places')
predictions_to_return = 5
preds = model.predict(image)[0]
top_preds = np.argsort(preds)[::-1][0:predictions_to_return]

# load the class label
file_name = 'categories_places365.txt'
if not os.access(file_name, os.W_OK):
    synset_url = 'https://raw.githubusercontent.com/csailvision/places365/master/categories_places365.txt'
    os.system('wget ' + synset_url)
classes = list()
with open(file_name) as class_file:
    for line in class_file:
        classes.append(line.strip().split(' ')[0][3:])
classes = tuple(classes)

# load the hierarchy lable and establish offset
csvContent = [x for x in csv.reader (open ('scene_hierarchy.csv','r'))] 
hierIdxOff = [idx for idx in range(0, 10) # within 10
              if "airfield" in csvContent[idx][0]][0]
classColumn = [csvContent[idx][0] for idx in range(len(csvContent))]
indoorColumn = [csvContent[idx][1] for idx in range(len(csvContent))]
outdoorNaturalColumn = [csvContent[idx][2] for idx in range(len(csvContent))]
outdoorManmadeColumn = [csvContent[idx][3] for idx in range(len(csvContent))]

print('--SCENE CATEGORIES:')
# output the prediction
indoorCount = 0
outdoorCount = 0
for i in range(0, 5):
    topClassIdx = top_preds[i]
    className = classes[top_preds[i]]
    print ("top %d" % i + ": " +  classes[topClassIdx])
    topHierIdx = topClassIdx + hierIdxOff
    assert (className in classColumn[topHierIdx])
    indoor_flg = int (indoorColumn [topHierIdx])
    outdoorManmade_flg = int (outdoorManmadeColumn [topHierIdx])
    outdoorNatural_flg = int (outdoorNaturalColumn [topHierIdx])
    assert (indoor_flg + outdoorNatural_flg + outdoorManmade_flg)
    if indoor_flg:
        print ("\tindoor")
    if outdoorManmade_flg:
        print ("\toutdoor man made")
    if outdoorNatural_flg:
        print ("\toutdoor natural")
    if indoor_flg:
        indoorCount += 1
    if outdoorNatural_flg or outdoorManmade_flg:
        outdoorCount += 1

# output the hierarchy ("indoor" vs "outdoor")
print('--SCENE HIERARCHY:')
if indoorCount > outdoorCount:
    print ("Majority vote: indoor")
else:
    print ("Majority vote: outdoor")
