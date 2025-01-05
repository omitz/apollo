import keras_ocr

pipeline = keras_ocr.pipeline.Pipeline()

images = ['Army_Reserves_Recruitment_Banner_MOD_45156284.jpg']

prediction_groups = pipeline.recognize(images)

print(prediction_groups)

