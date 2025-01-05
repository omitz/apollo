import os
import pickle
import mpu.aws as mpuaws
import pandas as pd
import warnings
import subprocess
from tqdm import tqdm
from collections import defaultdict
import cv2
from apollo import Analytic, FileManager, S3FileStore, PostgresDatabase
from imageai.Detection.det_and_classify import VideoObjectDetection, get_od_prob_key, get_class_name_key
from select_for_face import select_persons_for_recognition

class ObjDetVidAnalytic(Analytic):
    def __init__(self, name, testing_in_jenkins=False):
        super().__init__(name)
        self.ram_storage = '/dev/shm'
        self.model_initialized = False
        if not testing_in_jenkins:
            self.s3filestore = S3FileStore()
            self.modelpath = super().download_model('local/obj-det-vid/yolo.h5', self.s3filestore)
            self.init_model()
        self.obj_and_sec_prob_key = 'probability'
        self.obj_and_sec_sec_key = 'seconds'
        self.intermediate_output = None

    def init_model(self):
        print('Initializing detector', flush=True)
        self.detector = VideoObjectDetection()
        self.detector.setModelTypeAsYOLOv3()
        self.detector.setModelPath(self.modelpath)
        self.detector.loadModel(detection_speed='fast')
        self.model_initialized = True

    def file_check(self, local_path):
        # TODO Should this be a function in the analytic parent class?
        if not os.path.isfile(local_path):
            raise ValueError(f'{local_path} is not a valid file path.')
        size = os.stat(local_path).st_size
        if size == 0: # If file has 0 bytes
            raise ValueError(f'{local_path} is empty.')

    def get_rotation(self, local_path):
        # TODO Should this be a function in an image analytic parent class?
        # Get video rotation metadata
        rotation = None
        try:
            process = subprocess.Popen(['mediainfo', local_path],
                                       stdout=subprocess.PIPE,
                                       shell=False)
            metadata_bytes, err = process.communicate()
            metadata = metadata_bytes.decode('utf-8')
            # Parse the metadata
            df = pd.DataFrame([x.split(' : ') for x in metadata.split('\n')])
            df.columns = ['key', 'value']
            no_spaces = df.replace({' ': ''}, regex=True)
            rot_series = no_spaces.loc[no_spaces.key == 'Rotation', 'value']
            if len(rot_series) > 0:
                # Convert rotation string with degree symbol to int
                rotation = ''.join(e for e in rot_series.iloc[0] if e.isalnum())
                rotation = int(rotation)
        except Exception as e:
            msg = f'Unable to get rotation metadata for {local_path} due to exception:\n{e}. \nProceeding without rotating.'
            warnings.warn(msg)
        return rotation

    def detect(self, local_path):
        '''
        Args:
            local_path: The path to the video file in the docker container
        Returns:
            output_frames_dict: For every frame that we ran detection on, a dict for each individual detection
            objects_and_seconds: A dict where each entry is a class detected in this video, eg
                {'book': {'seconds': {1, 2, 3, 4, 5, 6}, 'probability': 60.20811200141907},
                 'person': {'seconds': {6, 7, 8, 9, 10, 11, 12}, 'probability': 98.64856004714966}}
        '''
        rotation = self.get_rotation(local_path)
        frame_detection_interval = 4
        if not self.model_initialized:
            self.init_model()
        self.detector.detectObjectsFromVideo(input_file_path=local_path,
                                                           frame_detection_interval=frame_detection_interval,
                                                           log_progress=True,
                                                           save_detected_video=False,
                                                           rotation=rotation)
        self.intermediate_output = self.detector.intermediate_output

        # In many cases, we'll need to free up some memory to post-process. We'll make this decision based on (very) rough estimate of how many objects were detected.
        size = sum(os.path.getsize(os.path.join(self.intermediate_output, f)) for f in os.listdir(self.intermediate_output))
        if size > 40000000:
            print('deleting detector', flush=True)
            del self.detector
            self.model_initialized = False
        output_frames_dict = self.load_all_frame_detections()

        objects_and_seconds = self.objects_by_second(local_path, output_frames_dict)

        # # Local debugging
        # with open('output_frames_dict.pkl', 'wb') as handle:
        #     pickle.dump(output_frames_dict, handle, protocol=pickle.HIGHEST_PROTOCOL)

        return output_frames_dict, objects_and_seconds

    def objects_by_second(self, local_path, output_frames_dict):
        print(f'Building objects-by-second dict for postgres...', flush=True)
        objects_and_seconds = defaultdict(lambda: {self.obj_and_sec_sec_key: set(), self.obj_and_sec_prob_key: 0})
        input_video = cv2.VideoCapture(local_path)
        frames_per_second = round(input_video.get(cv2.CAP_PROP_FPS))
        del input_video
        for i, (frame, detections) in tqdm(enumerate(output_frames_dict.items())):
            second = int(
                frame / frames_per_second) + 1  # + 1 because we'll call 00:00:00 - 00:00:01 second '1', 00:00:01 - 00:00:02 second '2', etc.
            for obj_dict in output_frames_dict[frame]:
                obj_class = obj_dict[get_class_name_key()]
                objects_and_seconds[obj_class][self.obj_and_sec_sec_key].add(second)
                prior_highest_prob = objects_and_seconds[obj_class][self.obj_and_sec_prob_key]
                prob = obj_dict[get_od_prob_key()]
                if prob > prior_highest_prob:
                    objects_and_seconds[obj_class][self.obj_and_sec_prob_key] = prob
        return objects_and_seconds

    def load_all_frame_detections(self):
        output_frames_dict = {}
        frame_dicts = os.listdir(self.intermediate_output)
        for frame_dict_file in tqdm(frame_dicts):
            full_path = os.path.join(self.intermediate_output, frame_dict_file)
            with open(full_path, 'rb') as handle:
                frame_dict = pickle.load(handle)
            # Grab the only key from the dictionary (which is the frame number)
            frame = list(frame_dict.keys())[0]
            output_frames_dict[frame] = frame_dict[frame]
        for file in frame_dicts:
            full_path = os.path.join(self.intermediate_output, file)
            os.remove(full_path)
        return output_frames_dict

    def run(self, filename, select_for_face=False):
        '''
        Given an S3 filepath, download the file and run it through the analytic.
        '''
        # Access S3 bucket
        bucket, target = mpuaws._s3_path_split(filename)
        self.s3filestore.download_file(target, self.ram_storage)

        base = os.path.basename(filename)
        local_path = os.path.join(self.ram_storage, base)

        self.file_check(local_path)

        output_frames_dict, objects_and_seconds = self.detect(local_path)
        if select_for_face:
            select_persons_for_recognition(output_frames_dict)
        return objects_and_seconds

    def cleanup(self):
        self.filemanager.cleanup()


