import sys
import os
import time

import mpu.aws as mpuaws
from PIL import Image

from apollo import Analytic, FileManager, S3FileStore, PostgresDatabase, MilvusHelper
from apollo.models import DetectedFace
from face.facenet.inference import extract_feature_vectors, read_mtcnn_results, rm_overlap, classify_and_threshold, draw_results, get_facenet_collection_name
from face.facenet.src.align.align_dataset_mtcnn import detect


S3_OUTPUT_DIR = 'outputs/face/'
APOLLO_FACENET_MODEL_FILE = 'local/facenet/model/clf.pkl'
RAM_STORAGE = '/dev/shm'
FACENET_SEARCH_THRESHOLD_DISTANCE = 1.2

class FacenetAnalytic(Analytic):

    def __init__(self, name, filestore=None):
        super().__init__(name)

        if filestore:
            self.filestore = filestore
        else:
            self.filestore = S3FileStore()

        # This if/else is useful during development and testing since the download is time-consuming
        full_model_path = os.path.join(RAM_STORAGE, 'clf.pkl')
        if not os.path.isfile(full_model_path):
            print('Downloading recognition model...', flush=True)
            self.recog_model = super().download_model(APOLLO_FACENET_MODEL_FILE, self.filestore)
            print('Finished downloading recognition model.', flush=True)
        else:
            self.recog_model = full_model_path

    def run(self, s3_filename):
    
        tic100 = time.perf_counter()

        local_output = os.path.join(RAM_STORAGE, 'outdir')

        if not os.path.exists(local_output):
            os.mkdir(local_output)

        print("processing " + s3_filename, file=sys.stderr)
        sys.stderr.flush()

        # Access S3 bucket
        bucket, target = mpuaws._s3_path_split(s3_filename)

        # Download the required file from the s3 bucket
        self.filestore.download_file(target, RAM_STORAGE)
        img_filename = os.path.split(target)[1]

        tic = time.perf_counter()
        
        ###############################
        # Run face detection
        local_img = os.path.join(RAM_STORAGE, img_filename)
        mtcnn_results_dir = detect(local_img)
        bbs = os.path.join(mtcnn_results_dir, 'bounding_boxes.txt')
        df = read_mtcnn_results(bbs)

        if len(df.index) == 0:
            return { 'dataframe': {}, 'embedding_array': [] }
        # df at this stage is a pandas dataframe of the output from MTCNN (the detection network). Each row represents one detected face. At this point the dataframe has columns for the filename and the four points that define the bounding box around the face.

        # Sometimes MTCNN detects the same face twice, so we'll remove any overlapping detections
        df = rm_overlap(df)

        # Get the arrays (one for each face). These arrays can be used to A) store in Milvus for future queries and/or B) use to run classification (ie getting the person's name)
        emb_array = extract_feature_vectors(df.path)
        best_class_prediction_thresholded, best_class_probabilities, colors = classify_and_threshold(self.recog_model, emb_array)
        
        df['probability'] = best_class_probabilities
        df['prediction'] = best_class_prediction_thresholded

        # So far, the df path column has been the local path for the face cropping, e.g. mtcnn_result/face_0.png, which can be useful for debugging. At this point, we can replace that path with the s3 path of the full image.
        num_rows = len(df.index)
        df['path'] = [s3_filename for i in range(num_rows)]
        

        im = draw_results(colors, df, os.path.join(RAM_STORAGE, img_filename))


        basename = os.path.basename(os.path.join(RAM_STORAGE, img_filename))
        base, _ = os.path.splitext(basename)

        # Save out the image with bounding boxes drawn onto it
        toc = time.perf_counter()
        print(f"\n!!! Inference processed image in {toc - tic:0.4f} seconds !!!!\n", flush=True)

        outfile_name = f'{base}_command_result.png'
        im.save(os.path.join(RAM_STORAGE, outfile_name))
        self.filestore.upload_file(os.path.join(RAM_STORAGE, outfile_name), os.path.join(S3_OUTPUT_DIR, outfile_name))
        ########################
        
        self.cleanup()
        toc100 = time.perf_counter()
        print(f"\n!!! TOTAL TOTAL TIME  took {toc100 - tic100:0.4f} seconds !!!!\n", flush=True)

        # For the sake of consistency with our other detection analytics/keeping our UI code simple, we'll convert the absolute bounding box coordinates to relative coordinates
        img = Image.open(local_img)
        width, height = img.size
        for col in ['ulx', 'lrx']:
            df[col] = df[col].apply(lambda x: x/width)
        for col in ['uly', 'lry']:
            df[col] = df[col].apply(lambda x: x/height)

        results_dict = dict()
        results_dict['dataframe'] = df.to_dict(orient='index')
        results_dict['embedding_array'] = emb_array
       
        return results_dict

    def get_closest_results(self, s3_filename, num_results=500):

        # Query Milvus for similar faces
        self.database = PostgresDatabase('apollo')

        milvus_helper = MilvusHelper(get_facenet_collection_name(), None, False)
        
        # Users should only submit images with one face in them. 
        # However, it's possible that faces in the background will be detected 
        # (eg target.png in which the subject is standing in front of a picture of someone). 
        # For now we'll handle this by assuming that the largest detection in the image is the one the user wanted to process. 
        # (One alternative would be to get the detection confidence from the detection model.)
        results_dict = self.run(s3_filename)
        df_dict = results_dict['dataframe']

        # Find the index of the largest detection
        areas = []
        for detection_int_key, det_val in df_dict.items():
            area = self.calc_area(det_val)
            areas.append(area)
        largest_size = max(areas)
        largest_idx = areas.index(largest_size)
        embedding_array = results_dict['embedding_array'][largest_idx]

        if len(embedding_array) > 0:
            milvus_results = milvus_helper.query(num_results, [embedding_array])[0]
            
            #slice milvus results to only within threshold distance
            for i in range(num_results):
                
                if i == len(milvus_results) or milvus_results[i].distance > FACENET_SEARCH_THRESHOLD_DISTANCE:
                    break
            milvus_results = milvus_results[:i]
            #

            print(f"num milvus_results: {len(milvus_results)}", flush=True)
            results = self.get_postgres_records(milvus_results)
            milvus_helper.close()            
            return results
        return []

    def calc_area(self, det_val):
        height = det_val['lry'] - det_val['uly']
        width = det_val['lrx'] - det_val['ulx']
        area = height * width
        return area

    def get_postgres_records(self, milvus_results):
        milvus_ids = [result.id for result in milvus_results]
        # Submit one query (rather than querying in a for-loop). Doing this in a single query supports scalability. The order won't be maintained, but we can sort afterward
        sql_results = self.database.query(DetectedFace, DetectedFace.vector_id.in_(milvus_ids)).all()
        # Sort the postgres results according to the order of the milvus_ids
        results = []
        for milvus_id in milvus_ids:
            # We expect only one sql row per Milvus id, but to be safe, we'll handle cases where the same Milvus id is in a table multiple times.
            sql_query_result = [sql_result for sql_result in sql_results if sql_result.vector_id == milvus_id]
            results += sql_query_result
        return results

    def cleanup(self):
        self.filemanager.cleanup()
