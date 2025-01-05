import json

from apollo import RabbitConsumer
from apollo import PostgresDatabase
from apollo.models import DetectedObj

from obj_det.models.research.object_detection.inference import format_for_postgres
from obj_det.models.research.object_detection.obj_det_utils.label_map_util import create_category_index_from_labelmap


class ObjectDetectionRabbitConsumer(RabbitConsumer):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.database = PostgresDatabase('apollo', DetectedObj.__table__)

    def save_results_to_database(self, msg_dict: dict, results: dict):

        category_index = create_category_index_from_labelmap('obj_det/models/research/object_detection/data/mscoco_complete_label_map.pbtxt', use_display_name=True)
        reduced_dict = format_for_postgres(msg_dict['name'], category_index, results)
        for i in range(len(reduced_dict['detection_class'])):
            detection = {}
            for k, v in reduced_dict.items():
                detection[k] = v[i]
            print(f"detection: {detection}", flush=True)

            if 'original_source' in msg_dict:
                detection['original_source'] = msg_dict['original_source']
            else:
                detection['original_source'] = detection['path']
            
            self.database.save_record_to_database(detection, DetectedObj)