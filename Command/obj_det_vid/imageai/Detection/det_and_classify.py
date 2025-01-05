import cv2
from imageai.Detection.utils.visualization import draw_box, draw_caption
from imageai.Detection.utils.colors import label_color
import numpy as np
import warnings
import tensorflow as tf
# Suppress warnings
tf.compat.v1.logging.set_verbosity(tf.compat.v1.logging.ERROR)
import os
from keras import backend as K
from keras.layers import Input
from PIL import Image
import colorsys
import pickle

from imageai.Detection.YOLOv3.models import yolo_main
from imageai.Detection.YOLOv3.utils import letterbox_image, yolo_eval


class ObjectDetection:
    '''
    This is the object detection class for images in the ImageAI library. It provides support for YOLOv3 object detection networks . After instantiating this class, you can set it's properties and
     make object detections using it's pre-defined functions.

     The following functions are required to be called before object detection can be made
     * setModelPath()
     * At least of of the following and it must correspond to the model set in the setModelPath()
      [setModelTypeAsYOLOv3()]
     * loadModel() [This must be called once only before performing object detection]

     Once the above functions have been called, you can call the detectObjectsFromImage() function of
     the object detection instance object at anytime to obtain observable objects in any image.
    '''

    def __init__(self):
        self.__modelType = ""
        self.modelPath = ""
        self.__modelPathAdded = False
        self.__modelLoaded = False
        self.__model_collection = []

        self.numbers_to_names = {0: get_person_class(), 1: 'bicycle', 2: 'car', 3: 'motorcycle', 4: 'airplane', 5: 'bus',
                                 6: 'train',
                                 7: 'truck', 8: 'boat', 9: 'traffic light', 10: 'fire hydrant', 11: 'stop sign',
                                 12: 'parking meter',
                                 13: 'bench', 14: 'bird', 15: 'cat', 16: 'dog', 17: 'horse', 18: 'sheep', 19: 'cow',
                                 20: 'elephant',
                                 21: 'bear', 22: 'zebra', 23: 'giraffe', 24: 'backpack', 25: 'umbrella', 26: 'handbag',
                                 27: 'tie',
                                 28: 'suitcase', 29: 'frisbee', 30: 'skis', 31: 'snowboard', 32: 'sports ball',
                                 33: 'kite',
                                 34: 'baseball bat', 35: 'baseball glove', 36: 'skateboard', 37: 'surfboard',
                                 38: 'tennis racket',
                                 39: 'bottle', 40: 'wine glass', 41: 'cup', 42: 'fork', 43: 'knife', 44: 'spoon',
                                 45: 'bowl',
                                 46: 'banana', 47: 'apple', 48: 'sandwich', 49: 'orange', 50: 'broccoli', 51: 'carrot',
                                 52: 'hot dog',
                                 53: 'pizza', 54: 'donut', 55: 'cake', 56: 'chair', 57: 'couch', 58: 'potted plant',
                                 59: 'bed',
                                 60: 'dining table', 61: 'toilet', 62: 'tv', 63: 'laptop', 64: 'mouse', 65: 'remote',
                                 66: 'keyboard',
                                 67: 'cell phone', 68: 'microwave', 69: 'oven', 70: 'toaster', 71: 'sink',
                                 72: 'refrigerator',
                                 73: 'book', 74: 'clock', 75: 'vase', 76: 'scissors', 77: 'teddy bear',
                                 78: 'hair dryer',
                                 79: 'toothbrush'}

        # Unique instance variables for YOLOv3
        self.__yolo_iou = 0.45
        self.__yolo_score = 0.1
        self.__yolo_anchors = np.array(
            [[10., 13.], [16., 30.], [33., 23.], [30., 61.], [62., 45.], [59., 119.], [116., 90.], [156., 198.],
             [373., 326.]])
        self.__yolo_model_image_size = (416, 416)
        self.__yolo_boxes, self.__yolo_scores, self.__yolo_classes = "", "", ""
        self.sess = K.get_session()

    def setModelTypeAsYOLOv3(self):
        """
                'setModelTypeAsYOLOv3()' is used to set the model type to the YOLOv3 model
                for the video object detection instance instance object .
                :return:
                """

        self.__modelType = "yolov3"

    def setModelPath(self, model_path):
        """
         'setModelPath()' function is required and is used to set the file path to a RetinaNet
          object detection model trained on the COCO dataset.
          :param model_path:
          :return:
        """

        if (self.__modelPathAdded == False):
            self.modelPath = model_path
            self.__modelPathAdded = True

    def loadModel(self, detection_speed="normal"):
        """
                'loadModel()' function is required and is used to load the model structure into the program from the file path defined
                in the setModelPath() function. This function receives an optional value which is "detection_speed".
                The value is used to reduce the time it takes to detect objects in an image, down to about a 10% of the normal time, with
                 with just slight reduction in the number of objects detected.


                * prediction_speed (optional); Acceptable values are "normal", "fast", "faster", "fastest" and "flash"

                :param detection_speed:
                :return:
        """

        if (self.__modelType == "yolov3"):
            if (detection_speed == "normal"):
                self.__yolo_model_image_size = (416, 416)
            elif (detection_speed == "fast"):
                self.__yolo_model_image_size = (320, 320)
            elif (detection_speed == "faster"):
                self.__yolo_model_image_size = (208, 208)
            elif (detection_speed == "fastest"):
                self.__yolo_model_image_size = (128, 128)
            elif (detection_speed == "flash"):
                self.__yolo_model_image_size = (96, 96)

        if (self.__modelLoaded == False):
            if (self.__modelType == ""):
                raise ValueError("You must set a valid model type before loading the model.")
            elif (self.__modelType == "yolov3"):
                model = yolo_main(Input(shape=(None, None, 3)), len(self.__yolo_anchors) // 3,
                                  len(self.numbers_to_names))
                model.load_weights(self.modelPath)

                hsv_tuples = [(x / len(self.numbers_to_names), 1., 1.)
                              for x in range(len(self.numbers_to_names))]
                self.colors = list(map(lambda x: colorsys.hsv_to_rgb(*x), hsv_tuples))
                self.colors = list(
                    map(lambda x: (int(x[0] * 255), int(x[1] * 255), int(x[2] * 255)),
                        self.colors))
                np.random.seed(10101)
                np.random.shuffle(self.colors)
                np.random.seed(None)

                self.__yolo_input_image_shape = K.placeholder(shape=(2,))
                self.__yolo_boxes, self.__yolo_scores, self.__yolo_classes = yolo_eval(model.output,
                                                                                       self.__yolo_anchors,
                                                                                       len(self.numbers_to_names),
                                                                                       self.__yolo_input_image_shape,
                                                                                       score_threshold=self.__yolo_score,
                                                                                       iou_threshold=self.__yolo_iou)

                self.__model_collection.append(model)
                self.__modelLoaded = True

    def detectObjectsFromImage(self, input_image, output_image_path="people",
                               extract_detected_objects=True, minimum_percentage_probability=50,
                               display_percentage_probability=True, display_object_name=True, thread_safe=False):
        """
            'detectObjectsFromImage()' function is used to detect objects observable in the given image path:
                    * input_image , which can be a numpy array
                    * output_image_path (only if output_type = file) , file path to the output image that will contain the detection boxes and label, if output_type="file"
                    * input_type (optional) , file path/numpy array/image file stream of the image. Acceptable values are "file", "array" and "stream"
                    * output_type (optional) , file path/numpy array/image file stream of the image. Acceptable values are "file" and "array"
                    * extract_detected_objects (optional) , option to save each object detected individually as an image and return an array of the objects' image path.
                    * minimum_percentage_probability (optional, 50 by default) , option to set the minimum percentage probability for nominating a detected object for output.
                    * display_percentage_probability (optional, True by default), option to show or hide the percentage probability of each object in the saved/returned detected image
                    * display_display_object_name (optional, True by default), option to show or hide the name of each object in the saved/returned detected image
                    * thread_safe (optional, False by default), enforce the loaded detection model works across all threads if set to true, made possible by forcing all Tensorflow inference to run on the default graph.


            The values returned by this function depends on the parameters parsed. The possible values returnable
            are stated as below
            - If extract_detected_objects = False or at its default value and output_type = 'file' or
                at its default value, you must parse in the 'output_image_path' as a string to the path you want
                the detected image to be saved. Then the function will return:
                1. an array of dictionaries, with each dictionary corresponding to the objects
                    detected in the image. Each dictionary contains the following property:
                    * name (string)
                    * percentage_probability (float)
                    * box_points (list of x1,y1,x2 and y2 coordinates)

            - If extract_detected_objects = False or at its default value and output_type = 'array' ,
              Then the function will return:

                1. a numpy array of the detected image
                2. an array of dictionaries, with each dictionary corresponding to the objects
                    detected in the image. Each dictionary contains the following property:
                    * name (string)
                    * percentage_probability (float)
                    * box_points (list of x1,y1,x2 and y2 coordinates)

            - If extract_detected_objects = True and output_type = 'file' or
                at its default value, you must parse in the 'output_image_path' as a string to the path you want
                the detected image to be saved. Then the function will return:
                1. an array of dictionaries, with each dictionary corresponding to the objects
                    detected in the image. Each dictionary contains the following property:
                    * name (string)
                    * percentage_probability (float)
                    * box_points (list of x1,y1,x2 and y2 coordinates)
                2. an array of string paths to the image of each object extracted from the image

            - If extract_detected_objects = True and output_type = 'array', the the function will return:
                1. a numpy array of the detected image
                2. an array of dictionaries, with each dictionary corresponding to the objects
                    detected in the image. Each dictionary contains the following property:
                    * name (string)
                    * percentage_probability (float)
                    * box_points (list of x1,y1,x2 and y2 coordinates)
                3. an array of numpy arrays of each object detected in the image
        """

        if (self.__modelLoaded == False):
            raise ValueError("You must call the loadModel() function before making object detection.")
        elif (self.__modelLoaded == True):
            try:
                if (self.__modelType == "yolov3" or self.__modelType == "tinyyolov3"):

                    output_objects_array = []

                    image = Image.fromarray(np.uint8(input_image))

                    detected_copy = input_image.copy()

                    new_image_size = (self.__yolo_model_image_size[0] - (self.__yolo_model_image_size[0] % 32),
                                      self.__yolo_model_image_size[1] - (self.__yolo_model_image_size[1] % 32))
                    boxed_image = letterbox_image(image, new_image_size)
                    image_data = np.array(boxed_image, dtype="float32")

                    image_data /= 255.
                    image_data = np.expand_dims(image_data, 0)

                    model = self.__model_collection[0]

                    if thread_safe == True:
                        with self.sess.graph.as_default():
                            out_boxes, out_scores, out_classes = self.sess.run(
                                [self.__yolo_boxes, self.__yolo_scores, self.__yolo_classes],
                                feed_dict={
                                    model.input: image_data,
                                    self.__yolo_input_image_shape: [image.size[1], image.size[0]],
                                    K.learning_phase(): 0
                                })
                    else:
                        out_boxes, out_scores, out_classes = self.sess.run(
                            [self.__yolo_boxes, self.__yolo_scores, self.__yolo_classes],
                            feed_dict={
                                model.input: image_data,
                                self.__yolo_input_image_shape: [image.size[1], image.size[0]],
                                K.learning_phase(): 0
                            })

                    min_probability = minimum_percentage_probability / 100
                    counting = 0

                    # Iterate over each individual detection in this frame
                    for a, b in reversed(list(enumerate(out_classes))):
                        predicted_class = self.numbers_to_names[b]
                        box = out_boxes[a]
                        score = out_scores[a]

                        if score < min_probability:
                            continue

                        counting += 1

                        objects_dir = output_image_path + "-objects"
                        if extract_detected_objects == True:
                            if (os.path.exists(objects_dir) == False):
                                os.mkdir(objects_dir)

                        label = "{} {:.2f}".format(predicted_class, score)

                        top, left, bottom, right = box
                        top = max(0, np.floor(top + 0.5).astype('int32'))
                        left = max(0, np.floor(left + 0.5).astype('int32'))
                        bottom = min(image.size[1], np.floor(bottom + 0.5).astype('int32'))
                        right = min(image.size[0], np.floor(right + 0.5).astype('int32'))

                        try:
                            color = label_color(b)
                        except:
                            color = (255, 0, 0)

                        detection_details = [left, top, right, bottom]
                        draw_box(detected_copy, detection_details, color=color)

                        if (display_object_name == True and display_percentage_probability == True):
                            draw_caption(detected_copy, detection_details, label)
                        elif (display_object_name == True):
                            draw_caption(detected_copy, detection_details, predicted_class)
                        elif (display_percentage_probability == True):
                            draw_caption(detected_copy, detection_details, str(score * 100))

                        each_object_details = {}
                        each_object_details[get_class_name_key()] = predicted_class
                        each_object_details[get_od_prob_key()] = score * 100
                        each_object_details["box_points"] = detection_details
                        if extract_detected_objects == True and predicted_class == get_person_class() and score >= .995: # Why .995? This was chosen arbitrarily and this method of select persons for face recognition should ultimately be revisited.
                            detected_copy2 = input_image.copy()
                            # Get the np array for just this object
                            splitted_copy = detected_copy2[detection_details[1]:detection_details[3],
                                            detection_details[0]:detection_details[2]]
                            each_object_details[get_img_arr_key()] = splitted_copy
                        output_objects_array.append(each_object_details)
                    return detected_copy, output_objects_array
            except:
                raise ValueError(
                    "Ensure you specified correct input image, input type, output type and/or output image path ")


class VideoObjectDetection:
    """
                    This is the object detection class for videos and camera live stream inputs in the ImageAI library. It provides support for RetinaNet,
                     YOLOv3 and TinyYOLOv3 object detection networks. After instantiating this class, you can set it's properties and
                     make object detections using it's pre-defined functions.

                     The following functions are required to be called before object detection can be made
                     * setModelPath()
                     * At least of of the following and it must correspond to the model set in the setModelPath()
                      [setModelTypeAsRetinaNet(), setModelTypeAsYOLOv3(), setModelTinyYOLOv3()]
                     * loadModel() [This must be called once only before performing object detection]

                     Once the above functions have been called, you can call the detectObjectsFromVideo() function
                     or the detectCustomObjectsFromVideo() of  the object detection instance object at anytime to
                     obtain observable objects in any video or camera live stream.
    """

    def __init__(self):
        self.__modelType = ""
        self.modelPath = ""
        self.__modelPathAdded = False
        self.__modelLoaded = False
        self.__detector = None
        self.__input_image_min = 1333
        self.__input_image_max = 800
        self.__detection_storage = None
        self.seconds_key = 'seconds'
        self.prob_key = 'probability'
        self.intermediate_output = 'intermediate_output'

        self.numbers_to_names = {0: get_person_class(), 1: 'bicycle', 2: 'car', 3: 'motorcycle', 4: 'airplane', 5: 'bus',
                                 6: 'train',
                                 7: 'truck', 8: 'boat', 9: 'traffic light', 10: 'fire hydrant', 11: 'stop sign',
                                 12: 'parking meter',
                                 13: 'bench', 14: 'bird', 15: 'cat', 16: 'dog', 17: 'horse', 18: 'sheep', 19: 'cow',
                                 20: 'elephant',
                                 21: 'bear', 22: 'zebra', 23: 'giraffe', 24: 'backpack', 25: 'umbrella', 26: 'handbag',
                                 27: 'tie',
                                 28: 'suitcase', 29: 'frisbee', 30: 'skis', 31: 'snowboard', 32: 'sports ball',
                                 33: 'kite',
                                 34: 'baseball bat', 35: 'baseball glove', 36: 'skateboard', 37: 'surfboard',
                                 38: 'tennis racket',
                                 39: 'bottle', 40: 'wine glass', 41: 'cup', 42: 'fork', 43: 'knife', 44: 'spoon',
                                 45: 'bowl',
                                 46: 'banana', 47: 'apple', 48: 'sandwich', 49: 'orange', 50: 'broccoli', 51: 'carrot',
                                 52: 'hot dog',
                                 53: 'pizza', 54: 'donut', 55: 'cake', 56: 'chair', 57: 'couch', 58: 'potted plant',
                                 59: 'bed',
                                 60: 'dining table', 61: 'toilet', 62: 'tv', 63: 'laptop', 64: 'mouse', 65: 'remote',
                                 66: 'keyboard',
                                 67: 'cell phone', 68: 'microwave', 69: 'oven', 70: 'toaster', 71: 'sink',
                                 72: 'refrigerator',
                                 73: 'book', 74: 'clock', 75: 'vase', 76: 'scissors', 77: 'teddy bear',
                                 78: 'hair dryer',
                                 79: 'toothbrush'}

        # Unique instance variables for YOLOv3 model
        self.__yolo_iou = 0.45
        self.__yolo_score = 0.1
        self.__yolo_anchors = np.array(
            [[10., 13.], [16., 30.], [33., 23.], [30., 61.], [62., 45.], [59., 119.], [116., 90.], [156., 198.],
             [373., 326.]])
        self.__yolo_model_image_size = (416, 416)
        self.__yolo_boxes, self.__yolo_scores, self.__yolo_classes = "", "", ""
        self.sess = K.get_session()

    def setModelTypeAsYOLOv3(self):
        """
                'setModelTypeAsYOLOv3()' is used to set the model type to the YOLOv3 model
                for the video object detection instance instance object .
                :return:
                """
        self.__modelType = "yolov3"

    def setModelPath(self, model_path):
        """
         'setModelPath()' function is required and is used to set the file path to a
         YOLOv3 object detection model trained on the COCO dataset.
          :param model_path:
          :return:
        """

        if (self.__modelPathAdded == False):
            self.modelPath = model_path
            self.__modelPathAdded = True

    def loadModel(self, detection_speed="normal"):
        """
                'loadModel()' function is required and is used to load the model structure into the program from the file path defined
                in the setModelPath() function. This function receives an optional value which is "detection_speed".
                The value is used to reduce the time it takes to detect objects in an image, down to about a 10% of the normal time, with
                 with just slight reduction in the number of objects detected.


                * prediction_speed (optional); Acceptable values are "normal", "fast", "faster", "fastest" and "flash"

                :param detection_speed:
                :return:
        """

        if (self.__modelLoaded == False):

            frame_detector = ObjectDetection()

            # if (self.__modelType == "retinanet"):
            #     frame_detector.setModelTypeAsRetinaNet()
            if (self.__modelType == "yolov3"):
                frame_detector.setModelTypeAsYOLOv3()
            frame_detector.setModelPath(self.modelPath)
            frame_detector.loadModel(detection_speed)
            self.__detector = frame_detector
            self.__modelLoaded = True


    def detectObjectsFromVideo(self, input_file_path="", output_file_path="",
                               frame_detection_interval=1, minimum_percentage_probability=50, log_progress=False,
                               display_percentage_probability=True, display_object_name=True, save_detected_video=True,
                               rotation=None):
        '''
        'detectObjectsFromVideo()' function is used to detect objects observable in the given video path
        Args:
            input_file_path: the file path to the input video
            output_file_path: the path to the output video. It is required only if 'save_detected_video' is not set to False
            frame_detection_interval: (optional, 1 by default), the intervals of frames that will be detected
            minimum_percentage_probability: (optional, 50 by default), option to set the minimum percentage probability for nominating a detected object for output
            log_progress: (optional), states if the progress of the frame processed is to be logged to console
            display_percentage_probability: (optional), can be used to hide or show probability scores on the detected video frames
            display_object_name: (optional), can be used to show or hide object names on the detected video frames
            save_detected_video: (optional, True by default), can be set to or not to save the detected video
            rotation: degrees the video needs to be rotated to be upright
        Returns:
            objects_and_seconds: a dict where each entry is a class detected in this video
            output_video_filepath: (optional) the video with detections drawn on
        '''
        if (input_file_path == ''):
            raise ValueError(
                "You must set 'input_file_path' to a valid video file")
        elif save_detected_video and output_file_path == "":
            raise ValueError(
                "You must set 'output_video_filepath' to a valid video file name, in which the detected video will be saved. If you don't intend to save the detected video, set 'save_detected_video=False'")
        else:
            try:
                input_video = cv2.VideoCapture(input_file_path)
                if log_progress:
                    total_frames = int(input_video.get(cv2.CAP_PROP_POS_FRAMES))
                frames_per_second = round(input_video.get(cv2.CAP_PROP_FPS))

                output_video_filepath = output_file_path + '.avi'

                if save_detected_video:

                    if rotation in [None, 180]:
                        frame_width = int(input_video.get(3))
                        frame_height = int(input_video.get(4))
                    elif rotation in [90, 270]:
                        frame_width = int(input_video.get(4))
                        frame_height = int(input_video.get(3))
                    else:
                        warnings.warn(f'Unexpected rotation argument {rotation}. Ignoring rotation.')
                        frame_width = int(input_video.get(3))
                        frame_height = int(input_video.get(4))

                    output_video = cv2.VideoWriter(output_video_filepath, cv2.VideoWriter_fourcc('M', 'J', 'P', 'G'),
                                                   frames_per_second,
                                                   (frame_width, frame_height))

                video_frames_count = 0

                if not os.path.isdir(self.intermediate_output):
                    os.mkdir(self.intermediate_output)
                previous_run = os.listdir(self.intermediate_output)
                for file in previous_run:
                    full_path = os.path.join(self.intermediate_output, file)
                    os.remove(full_path)

                while (input_video.isOpened()):
                    ret, frame = input_video.read()
                    if (ret == True):
                        video_frames_count += 1
                        if log_progress == True and video_frames_count % 100 == 0:
                        
                            print(f'Processing frame: {video_frames_count}/{total_frames}', flush=True)
                        check_frame_interval = video_frames_count % frame_detection_interval

                        if check_frame_interval == 0:
                            frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                            frame = rotate_frame(frame, rotation)
                            detected_copy, output_objects_array = self.__detector.detectObjectsFromImage(
                                frame,
                                minimum_percentage_probability=minimum_percentage_probability,
                                display_percentage_probability=display_percentage_probability,
                                display_object_name=display_object_name)

                            frame_dict = {}
                            frame_dict[video_frames_count] = output_objects_array

                            # If we store all detections from all frames at the same time, we'll run out of memory
                            with open(f'{self.intermediate_output}/objects_in_frame_{video_frames_count}.pkl', 'wb') as handle:
                                pickle.dump(frame_dict, handle, protocol=pickle.HIGHEST_PROTOCOL)

                            if save_detected_video:
                                detected_copy = cv2.cvtColor(detected_copy, cv2.COLOR_RGB2BGR)
                                output_video.write(detected_copy)
                    else:
                        break

                input_video.release()
                if save_detected_video:
                    output_video.release()
                    return output_video_filepath
                else:
                    return

            except:
                raise ValueError(
                    "An error occured. It may be that your input video is invalid. Ensure you specified a proper string value for 'output_file_path' is 'save_detected_video' is not False. "
                    "Also ensure your per_frame, per_second, per_minute or video_complete_analysis function is properly configured to receive the right parameters. ")


def rotate_frame(frame, rotation):
    if rotation is None:
        pass
    elif rotation == 90:
        frame = cv2.rotate(frame, cv2.ROTATE_90_CLOCKWISE)
    elif rotation == 180:
        frame = cv2.rotate(frame, cv2.ROTATE_180)
    elif rotation == 270:
        frame = cv2.rotate(frame, cv2.ROTATE_90_COUNTERCLOCKWISE)
    else:
        warnings.warn(f'Unexpected rotation argument {rotation}. Ignoring rotation.')
    return frame


def get_od_prob_key():
    return 'probability'


def get_person_class():
    return 'person'


def get_img_arr_key():
    return 'image_array'


def get_class_name_key():
    return 'name'


def get_session():
    config = tf.ConfigProto()
    config.gpu_options.allow_growth = True
    return tf.Session(config=config)
