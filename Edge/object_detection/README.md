## Object Detection and Classification

### Environment setup

Follow instructions here https://github.com/tensorflow/examples/tree/master/lite/examples/object_detection/android

### Project description

This repo contains the TensorFlow Lite Object Detection Android Demo with the following changes:
* Gave app write permissions
* Save out the image as a png file on the device
* Write out the object detection results to a json file on the device
* The app, by default, continuously runs inference on the camera preview displayed on the screen. With write permissions, the image gets saved out as preview.png and gets continuously overwritten with the most recent camera input.   
A button has been added so that, when clicked, the current image and its results will be saved out with unique filenames, e.g. 196.png and results_196.json. These get saved on the internal storage to /storage/emulated/0/tensorflow. On the device, this is accessed by navigating to My Files > tensorflow.

### User process
* Open TFL Detect
* Aim camera at scene of interest
* Click grey button at top of screen
* To copy the image and results to a computer
    * Connect the phone via USB
    * Make sure phone is in developer mode in order to use ```adb```
    * Get the device number by listing devices: ```adb devices```
    * ```adb -s <device number> pull /storage/emulated/0/tensorflow <destination>```
    
