#!/usr/bin/env python
#
# Without GPU, do:
#
#   env CUDA_VISIBLE_DEVICES="" ipython3
#

## Load the VGGVox keras model
import constants_apollo as c
from model_lite import vggvox_model
import numpy as np

model = vggvox_model ()
model.load_weights (c.WEIGHTS_FILE)
model.summary () 

###############
## Save the model as saved model:
###############
import tensorflow as tf
export_dir="/tmp/saved_model/1"
tf.saved_model.save (model, export_dir)
## Method 1:
# converter = tf.lite.TFLiteConverter.from_saved_model (export_dir)
## Method 2:
saved_model = tf.saved_model.load(export_dir)
concrete_func = saved_model.signatures[
    tf.saved_model.DEFAULT_SERVING_SIGNATURE_DEF_KEY]
concrete_func.inputs[0].set_shape([1, 512, 400, 1])
converter = tf.lite.TFLiteConverter.from_concrete_functions([concrete_func])
## Method 3:
# converter = tf.compat.v1.lite.TFLiteConverter.from_saved_model (
#     export_dir, input_shapes={'serving_default_input':[1, 512, 400, 1]})

## Convert to tensorflow lite:
tflite_model = converter.convert()
tflite_model_file = 'saved_model.tflite'
with open(tflite_model_file, 'wb') as f:
    f.write (tflite_model)


# ###############
# ## Save the keras model:
# ###############
# kerasModel_file="/tmp/model.h5"
# model.save (kerasModel_file)
# ## Method 1:
# # converter = tf.lite.TFLiteConverter.from_keras_model (model)
# ## Method 2:
# converter = tf.compat.v1.lite.TFLiteConverter.from_keras_model_file (
#     kerasModel_file, input_shapes={'input':[1, 512, 400, 1]})

# ## Convert to tensorflow lite:
# tflite_model = converter.convert()
# tflite_model_file = 'saved_model.tflite'
# with open(tflite_model_file, 'wb') as f:
#     f.write (tflite_model)


###############
## Load TFLite model and allocate tensors.
###############
## Method 1:
#interpreter = tf.lite.Interpreter (model_content = tflite_model)
## Method 2:
interpreter = tf.lite.Interpreter (model_path = tflite_model_file)
interpreter.allocate_tensors ()


###############
## Test the TensorFlow Lite model on random input data.
###############
# Get input and output tensors.
input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()

input_shape = input_details[0]['shape']
input_data = np.array (np.random.random_sample(input_shape),
                       dtype=input_details[0]['dtype'])
interpreter.set_tensor (input_details[0]['index'], input_data)

interpreter.invoke()
tflite_results = interpreter.get_tensor (output_details[0]['index'])
output_data = np.array (tflite_results)
print (output_data)
