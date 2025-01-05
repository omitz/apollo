import os, sys
import pprint
from collections import defaultdict
import mpu.aws as mpuaws
import numpy as np
import math
from PIL import Image
import cv2
import tensorflow as tf
from mtcnn import MTCNN

from apollo import Analytic, FileManager, S3FileStore, PostgresDatabase
from face.facenet.inference import classify_and_threshold
from face.facenet.src import facenet as fn
from face.facenet_video_rabbit_consumer.sort_tracker.sort import Sort


S3_OUTPUT_DIR = 'outputs/face/'
APOLLO_FACENET_MODEL_FILE = 'local/facenet/model/clf.pkl'
RAM_STORAGE = '/dev/shm'


class FacenetVideoAnalytic(Analytic):

    def __init__(self, name, filestore=None, download_recog_model=True):
        super().__init__(name)

        if filestore:
            self.filestore = filestore
        else:
            self.filestore = S3FileStore()

        # Making the model download optional allows us to quickly test non-recognition functionality
        if download_recog_model:
            # This if/else is useful during development since the download is time-consuming
            full_model_path = os.path.join(RAM_STORAGE, 'clf.pkl')
            if not os.path.isfile(full_model_path):
                print('Downloading recognition model...', flush=True)
                self.recog_model = super().download_model(APOLLO_FACENET_MODEL_FILE, self.filestore)
                print('Finished downloading recognition model.', flush=True)
            else:
                self.recog_model = full_model_path

    def run(self, s3_filename):
        print(f'Processing {s3_filename}', flush=True)
        # Access S3 bucket
        bucket, target = mpuaws._s3_path_split(s3_filename)
        # Download the required file from the s3 bucket
        self.filestore.download_file(target, RAM_STORAGE)
        img_filename = os.path.split(target)[1]
        local_vid = os.path.join(RAM_STORAGE, img_filename)
        results_dict = self.detect_and_recog(local_vid)
        return results_dict

    def detect_and_recog(self, local_vid, debug=False):
        input_video = cv2.VideoCapture(local_vid)
        frames_per_second = round(input_video.get(cv2.CAP_PROP_FPS))

        unique_detections = self.detect_and_track(input_video)

        if not unique_detections: # ie if there were no faces detected
            return {}

        # Run recognition
        with tf.Graph().as_default():
            with tf.compat.v1.Session() as sess:
                fn.load_model('face/facenet/src/models/downloaded/20180408-102900/20180408-102900.pb')
                # Get input and output tensors
                images_placeholder = tf.compat.v1.get_default_graph().get_tensor_by_name("input:0")
                embeddings_tensor = tf.compat.v1.get_default_graph().get_tensor_by_name("embeddings:0")
                phase_train_placeholder = tf.compat.v1.get_default_graph().get_tensor_by_name("phase_train:0")

                # Do the preprocessing required to pass the face as input to the recognition network
                faces4d = self.resize_faces(unique_detections['face_arr'])

                feed_dict = {images_placeholder: faces4d, phase_train_placeholder: False}
                emb_array = sess.run(embeddings_tensor, feed_dict=feed_dict)
                best_class_prediction_thresholded, best_class_probabilities, colors = classify_and_threshold(
                    self.recog_model, emb_array)

                unique_detections['predictions'] = best_class_prediction_thresholded
                unique_detections['recog_probability'] = best_class_probabilities
        if debug:
            # Save out the faces that were passed to the recognition network and their predictions
            for i, face in enumerate(unique_detections['face_arr']):
                im = Image.fromarray(face)
                im.save(f'face/facenet_video_rabbit_consumer/{i}_{best_class_prediction_thresholded[i]}.jpg')
        # Format to store in postgres
        # Convert frame numbers to seconds
        seconds_all = []
        for frames_arr in unique_detections['frames']:
            seconds = [math.floor(frame / frames_per_second) for frame in frames_arr]
            seconds_all.append(seconds)
        unique_detections['seconds'] = seconds_all
        # Combine instances of KNOWN persons
        unique_known = set(unique_detections['predictions'])
        if 'Unknown' in unique_known:
            unique_known.remove('Unknown')
        results_dict = defaultdict(list)
        for person in unique_known:
            idxs = [i for i, pred in enumerate(unique_detections['predictions']) if pred == person]

            max_recog_probability = max([unique_detections['recog_probability'][i] for i in idxs])

            seconds = [unique_detections['seconds'][i] for i in idxs]
            seconds = [sec for sec_arr in seconds for sec in sec_arr]

            results_dict['prediction'].append(person)
            results_dict['recog_probability'].append(max_recog_probability)
            results_dict['seconds'].append(seconds)
        unknowns_idxs = [i for i, pred in enumerate(unique_detections['predictions']) if pred == 'Unknown']
        for i in unknowns_idxs:
            results_dict['prediction'].append('Unknown')
            results_dict['recog_probability'].append(unique_detections['recog_probability'][i])
            results_dict['seconds'].append(unique_detections['seconds'][i])
        pretty_printer = pprint.PrettyPrinter(indent=2)
        print('results_dict: ', flush=True)
        pretty_printer.pprint(results_dict)
        sys.stdout.flush()
        return results_dict

    def detect_and_track(self, input_video, visualize=False):
        '''
        :param input_video: cv2 VideoCapture
        '''
        frames_per_second = round(input_video.get(cv2.CAP_PROP_FPS))
        video_frames_count = 0
        detector = MTCNN()
        multi_obj_tracker = Sort()
        unique_detections = defaultdict(list)

        if visualize:
            frame_width = int(input_video.get(3))
            frame_height = int(input_video.get(4))
            output_video = cv2.VideoWriter('face/facenet_video_rabbit_consumer/output.avi', cv2.VideoWriter_fourcc('M', 'J', 'P', 'G'),
                                           frames_per_second,
                                           (frame_width, frame_height))

        while input_video.isOpened():
            ret, frame = input_video.read()
            if ret:
                # Check once per second
                if video_frames_count % frames_per_second == 0:
                    print(f'At frame {video_frames_count}.')
                    frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

                    if visualize:
                        copy_for_vis = frame.copy()

                    # Run face detection
                    bbs_keypoints_conf = detector.detect_faces(frame)
                    # Format the MTCNN detections to pass to the sort tracker
                    bbs_and_scores = []
                    for detection in bbs_keypoints_conf:
                        x, y, width, height = detection['box']
                        # It's possible for MTCNN to return a negative x and/or y
                        if x < 0: x = 0
                        if y < 0: y = 0
                        x1, y1, x2, y2, score = x, y, x + width, y + height, detection['confidence']
                        bbs_and_scores.append(np.array([x1, y1, x2, y2, score]))
                    bbs_and_scores = np.array(bbs_and_scores)

                    # Even if there aren't any detections, we must call SORT's 'update' func
                    # 'update' returns a np arr where each row contains a bb and (in the last column) track id
                    # NOTE: The number of objects returned may differ from the number of detections provided.
                    if len(bbs_and_scores) == 0:
                        track_bbs_ids = multi_obj_tracker.update()
                    else:
                        track_bbs_ids = multi_obj_tracker.update(bbs_and_scores)

                    for i, bb_id in enumerate(track_bbs_ids):

                        sortface = SortFace(bb_id, frame)
                        this_conf = bbs_and_scores[i][-1]

                        if visualize:
                            self.draw_box(copy_for_vis, sortface.box_list())

                        if sortface.id in unique_detections['ids']:  # If this detection is one we were previously tracking
                            self.update_unique_detections(sortface, this_conf, unique_detections, video_frames_count)
                        else:  # This is a new detection
                            unique_detections['ids'].append(sortface.id)
                            unique_detections['det_conf'].append(this_conf)
                            unique_detections['face_arr'].append(sortface.face)
                            # Keep track of WHEN we saw this person in the video
                            unique_detections['frames'].append([video_frames_count])

                    if visualize:
                        copy_for_vis = cv2.cvtColor(copy_for_vis, cv2.COLOR_RGB2BGR)
                        for i in range(frames_per_second):
                            output_video.write(copy_for_vis)

                video_frames_count += 1
            else:
                if visualize:
                    output_video.release()
                break

        if 'ids' in unique_detections.keys():
            num_detections = len(unique_detections['ids'])
            print(f'num detections: {num_detections}', flush=True)
            for k, v in unique_detections.items():
                assert len(v) == num_detections
        return unique_detections

    def update_unique_detections(self, sortface, this_conf, unique_detections, video_frames_count):
        # Update the idx'th entry in unique_detections
        idx = unique_detections['ids'].index(sortface.id)  # Where in unique_detections is this id
        # Save the highest-confidence face array to later pass to recognition
        prev_highest_conf = unique_detections['det_conf'][idx]
        if this_conf > prev_highest_conf:
            unique_detections['det_conf'][idx] = this_conf
            unique_detections['face_arr'][idx] = sortface.face
        unique_detections['frames'][idx].append(video_frames_count)

    def resize_faces(self, faces):
        '''
        Ie preprocess the face arrays. Ensures 3 channel format, runs normalization, resizes, and combine into one 4d array
        :param face: A list of numpy arrays
        :return: face4d: A numpy array
        '''
        image_size = 160
        faces4d = np.zeros((len(faces), image_size, image_size, 3))
        for i, face in enumerate(faces):
            if face.ndim == 2:
                face = fn.to_rgb(face)
            face = fn.prewhiten(face)
            res = cv2.resize(face, dsize=(image_size, image_size), interpolation=cv2.INTER_CUBIC)
            faces4d[i,:,:,:] = res
        return faces4d

    def draw_box(self, image, box, color=(255, 0, 0), thickness=2):
        """ Draws a box on an image with a given color.

        # Arguments
            image     : The image to draw on.
            box       : A list of 4 elements (x1, y1, x2, y2).
            color     : The color of the box.
            thickness : The thickness of the lines to draw a box with.
        """
        b = np.array(box).astype(int)
        cv2.rectangle(image, (b[0], b[1]), (b[2], b[3]), color, thickness, cv2.LINE_AA)

    def cleanup(self):
        self.filemanager.cleanup()


class SortFace:
    def __init__(self, bb_id, frame):
        self.id = bb_id[-1]
        self.x1, self.y1, self.x2, self.y2 = bb_id[:4].astype(int)
        self.face = self.extract_face(frame)

    def extract_face(self, frame):
        # Extract just the face from the frame
        face = frame[self.y1:self.y2, self.x1:self.x2, :]
        return face

    def box_list(self):
        return [self.x1, self.y1, self.x2, self.y2]