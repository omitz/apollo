from flask import Blueprint, Flask, current_app, jsonify, request
from flask_restful import Api, Resource
import os, pathlib
import time
import mpu.aws as mpuaws
from apollo import S3FileStore, FileManager
from extract_features import extract_feats
from match_images import get_sorted_matches


blueprint = Blueprint('api', __name__)
api = Api(blueprint)

class HealthCheckResource(Resource):
    def get(self):
        return {'hello': 'world'}


class LandmarkQuery():
    def __init__(self, descriptors, locations):
        self.delf_descriptors = descriptors
        self.delf_locations = locations


class FindResource(Resource):
    def get(self):
        s3filepath = request.args['name']

        print(f'\n Request = {request.args} \n', flush=True)

        file_manager = FileManager()
        s3filestore = S3FileStore()
        bucket, target = mpuaws._s3_path_split(s3filepath)
        s3filestore.download_file(target, file_manager.ram_storage)
        img_file = os.path.basename(s3filepath)
        img_file = os.path.join(file_manager.ram_storage, img_file)

        # Change working directory so we can load files relative to landmark_recognition
        this_file = pathlib.Path(os.path.dirname(os.path.abspath(__file__)))
        landmark = this_file.parents[1]
        os.chdir(landmark)

        results = self.match(img_file, s3filepath)
        return results

    def match(self, img_file, s3filepath):
        '''
        Args:
            img_file: The local image file
            s3filepath: The full s3 filepath
        '''
        num_inliers_threshold = 10

        extract_start = time.time()
        img_dicts, features_found = extract_feats([img_file], source_paths=[s3filepath])
        if not features_found:
            return jsonify({'landmarks': [], 'msg': 'No features were found in the query image.'})
        img_dict = img_dicts[0]
        extract_end = time.time()
        print(f'\nExtract time: {extract_end - extract_start}', flush=True)
        query = LandmarkQuery(img_dict['delf_descriptors'], img_dict['delf_locations'])

        match_start = time.time()
        matches_high_to_low = get_sorted_matches(query)
        match_end = time.time()
        print(f'\nMatch time: {match_end - match_start}', flush=True)

        thresholded_matches = [match for match in matches_high_to_low if match[0] > num_inliers_threshold]
        if len(thresholded_matches) == 0:
            print(f"no matches", flush=True)
            return jsonify({'landmarks': [], 'msg': 'No matches.'})
        matches = []
        for i, match in enumerate(thresholded_matches):
            print(f"match: {match}", flush=True)
            inliers, db_el = match
            match_dict = {'num_inliers': f'{inliers}', 'path': db_el.path, 'original_source': db_el.original_source}
            matches.append(match_dict)
        print(f"matches: {len(matches)}", flush=True)
        # Note: UI relies on 'landmarks' key
        results = jsonify({'landmarks': matches})
        return results

api.add_resource(HealthCheckResource, '/health')
api.add_resource(FindResource, '/search')
