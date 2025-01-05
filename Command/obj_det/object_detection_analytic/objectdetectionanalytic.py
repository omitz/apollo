import os
import json

import mpu.aws as mpuaws
from apollo import Analytic, FileManager, S3FileStore, PostgresDatabase
from apollo.models import DetectedObj

from obj_det.models.research.object_detection.inference import process


S3_OUTPUT_DIR = 'outputs/obj_det/'
RAM_STORAGE = '/dev/shm'

class ObjectDetectionAnalytic(Analytic):

    def __init__(self, name, filestore=None, filemanager=None):
        super().__init__(name)

        if filestore:
            self.filestore = filestore
        else:
            self.filestore = S3FileStore()

        if filemanager:
            self.filemanager = filemanager
        else:
            self.filemanager = FileManager()

        #self.model = super().download_model(APOLLO_FACENET_MODEL_FILE, self.filestore)

    def run(self, s3_filename):
        """
        :param s3_filename: eg "s3://apollo-source-data/inputs/obj_det/test.jpg"
        """

        print("processing " + s3_filename, flush=True)
        bucket, prefix = mpuaws._s3_path_split(s3_filename)
        self.filestore.download_file(prefix, self.filemanager.ram_storage)
        img_file = os.path.split(prefix)[1]

        results = process(os.path.join(self.filemanager.ram_storage, img_file), self.filemanager.local_outdir, visualize=True)

        self.filestore.upload_dir_content(self.filemanager.local_outdir, S3_OUTPUT_DIR)

        return results

    def get_closest_results(self, s3_filename, num_results=10):
        raise NotImplementedError

    def cleanup(self):
        self.filemanager.cleanup()