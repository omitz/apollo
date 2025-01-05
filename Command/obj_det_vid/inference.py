'''
Prototype for reference. All functionality has been moved to obj_det_vid_analytic and obj_det_vid_rabbit_consumer. In this example, the video with the detections gets saved out.
'''

from time import time
import os
import subprocess
import itertools
import pandas as pd
import warnings
from commandutils import postgres_utils
from imageai.Detection.det_and_classify import VideoObjectDetection


# vid = "videos/inputs/cars_trans1trans2.mp4"
vid = 'videos/inputs/red.mp4'
# vid = 'vid_clip.mp4'
# vid = 'videos/inputs/holo_clip.mp4'
# vid = '/home/HQ/lneff/apollo/tmp/TheFastandtheFuriousJohnIreland1954goofyrip_512kb.mp4'
# vid = '/home/HQ/lneff/Downloads/DJI_0058.MOV'
# vid = 'videos/inputs/cars.mp4'
# vid = 'videos/inputs/rot.mp4'
# vid = 'videos/inputs/rot90.mp4'
# vid = 'videos/inputs/rot180.mp4'
# vid = 'videos/inputs/rot270.mp4'

rotation = None

process = subprocess.Popen(['mediainfo', vid],
                           stdout=subprocess.PIPE,
                           shell=False)
metadata_bytes, err = process.communicate()
metadata = metadata_bytes.decode('utf-8')

try:
    df = pd.DataFrame([x.split(' : ') for x in metadata.split('\n')])
    df.columns = ['key', 'value']
    no_spaces = df.replace({' ':''}, regex=True)
    rot_series = no_spaces.loc[no_spaces.key == 'Rotation', 'value']
    if len(rot_series) > 0:
        rotation = ''.join(e for e in rot_series.iloc[0] if e.isalnum())
        rotation = int(rotation)
except Exception as e:
    msg = f'Unable to get rotation metadata for {vid} due to exception:\n{e}. \nProceeding without rotating.'
    warnings.warn(msg)

detector = VideoObjectDetection()
detector.setModelTypeAsYOLOv3()
detector.setModelPath('yolo.h5')
detector.loadModel(detection_speed='fast')

base = os.path.basename(vid)
name, ext = os.path.splitext(base)
out = f'videos/results/{name}_result'

start = time()
vid_path, detections_dict = detector.detectObjectsFromVideo(input_file_path=vid,
                                           output_file_path=out,
                                           frame_detection_interval=2, #50,
                                           log_progress=True,
                                            rotation=rotation)
print(f'detections dict: {detections_dict}')
end = time()

vid_duration = subprocess.run(['ffprobe', '-v', 'error', '-show_entries', 'format=duration', '-of', 'default=noprint_wrappers=1:nokey=1', vid],
                              stdout=subprocess.PIPE,
                              stderr=subprocess.STDOUT)
vid_duration = float(vid_duration.stdout)
processing_time = end-start
print(f'processing time: {processing_time}\nvid duration: {vid_duration}')
print(f'processing time per second of video: {processing_time/vid_duration}')

def get_ranges(sorted_list_of_ints):
    # https://stackoverflow.com/questions/4628333/converting-a-list-of-integers-into-range-in-python
    grouped = itertools.groupby(enumerate(sorted_list_of_ints), lambda pair: pair[1] - pair[0])
    for a, b in grouped:
        b = list(b)
        yield b[0][1], b[-1][1]

# Create a dictionary formatted for `save_record_to_database
sec_key = 'seconds'
prob_key = 'probability'
formatted = []
for obj_class, sec_prob in detections_dict.items():
    obj_class_data = {}
    obj_class_data['path'] = vid
    obj_class_data['detection_class'] = obj_class
    obj_class_data['detection_score'] = round(detections_dict[obj_class][prob_key], 2) # Round to 2 decimal places
    # Turn the seconds sets into ranges
    seconds_list = list(detections_dict[obj_class][sec_key])
    ranges = list(get_ranges(seconds_list))
    obj_class_data['seconds'] = ranges
    formatted.append(obj_class_data)

print(f'formatted: {formatted}')

from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy import Column, Integer, String, Float, BigInteger, ARRAY

Base = declarative_base()

class VideoDetections(Base):
    __tablename__ = 'video_detections'

    id = Column(BigInteger, primary_key=True, nullable=False)
    path = Column(String)
    detection_class = Column(String)
    detection_score = Column(Float)
    seconds = Column(ARRAY(Integer))

    def __repr__(self):
        return f"<VideoDetection  (id='{self.id}', path='{self.path}')>"


engine = postgres_utils.init_database(VideoDetections)

from time import sleep
sleep(3)
for obj_class in formatted:
    postgres_utils.save_record_to_database(engine, obj_class, VideoDetections)
engine.dispose()

'''
postgres cols:

each row is a unique object in the video, so this would be 

s3path
probability (here we store the max probability of any <object class> in the vid)
class
start-stop, start-stop, start-stop

s3://holo_clip.mp4  book    0.55    0-2, 5-6
s3://holo_clip.mp4  person    0.98    5-11, 12-13
s3://holo_clip.mp4  teddy bear    0.80    6

'''