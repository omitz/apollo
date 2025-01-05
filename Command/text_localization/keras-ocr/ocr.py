#!/usr/bin/env python3

import matplotlib.pyplot as plt
import keras_ocr

pipeline = keras_ocr.pipeline.Pipeline()

images = [ keras_ocr.tools.read('Army_Reserves_Recruitment_Banner_MOD_45156284.jpg') ]
prediction_groups = pipeline.recognize(images)

# Print the predictions -- text and bounding box
print(prediction_groups) 

# Plot the predictions [optional]
fig, ax = plt.subplots(nrows=1, figsize=(20, 20))
keras_ocr.tools.drawAnnotations(image=images[0], predictions=prediction_groups[0], ax=ax)
fig.savefig ("out.png")
