import sys, os
import argparse
import pickle
import tensorflow as tf
import math
import numpy as np
from tqdm import tqdm
import pandas as pd
from apollo import MilvusHelper

from .src.align.align_dataset_mtcnn import detect
from .src import facenet as fn
from .box_utils import rm_overlap, draw_results

'''
This script takes an image as input, detects faces in the image, then (for each face) computes the feature vector for the face, queries Milvus for similar vectors, optionally adds the face to the postgres table, and optionally adds the face to the Milvus database.
'''


## Database helper funcs -----------------------------

def save_df_to_postgres(engine, postgres_model, df):
    for row in df.T.to_dict().values():
        output(f'row: {row}')
        postgres_utils.save_record_to_database(engine, row, postgres_model)

def get_facenet_collection_name():
    # Milvus collection name
    collection = 'facenet'
    return collection
## ----------------------------------------------


def output(output):
    print(output, flush=True)


def main(args):
    full_size = args.file

    # Run face detection
    mtcnn_results_dir = detect(full_size)

    bbs = os.path.join(mtcnn_results_dir, 'bounding_boxes.txt')
    df = read_mtcnn_results(bbs)

    if len(df.index) == 0:
        return
    # df at this stage is a pandas dataframe of the output from MTCNN (the detection network). Each row represents one detected face. At this point the dataframe has columns for the filename and the four points that define the bounding box around the face.

    # Sometimes MTCNN detects the same face twice, so we'll remove any overlapping detections
    df = rm_overlap(df)

    # Get the arrays (one for each face). These arrays can be used to A) store in Milvus for future queries and/or B) use to run classification (ie getting the person's name)
    emb_array = extract_feature_vectors(df.path) # Note: the paths in the dataframe at this point are local filepaths. Each one is a face cropped out of an image (by MTCNN)
    # Clean up the face crops that were saved out by MTCNN (the detection network)
    for path in df.path:
        os.remove(path)

    # Predict who the person is (and the color of their bounding box) based on thresholds
    classification_model = args.model
    best_class_prediction_thresholded, best_class_probabilities, colors = classify_and_threshold(
        classification_model, emb_array)

    # Add the predictions to the dataframe
    df['probability'] = best_class_probabilities
    df['prediction'] = best_class_prediction_thresholded

    # So far, the df path column has been the local path for the face cropping, e.g. mtcnn_result/face_0.png, which can be useful for debugging. At this point, we can replace that path with the s3 path of the full image.
    num_rows = len(df.index)
    df['path'] = [args.source for i in range(num_rows)]

    # This is probably the point where you'd want to return the df (or a jsonified version)

    # Draw the bounding boxes and labels onto a copy of the image
    im = draw_results(colors, df, full_size)

    # Save results
    if not os.path.exists(args.output):
        os.mkdir(args.output)
    basename = os.path.basename(full_size)
    base, _ = os.path.splitext(basename)

    # Save out the image with bounding boxes drawn onto it
    outfile_name = f'{base}_command_result.png'
    im.save(os.path.join(args.output, outfile_name))

# -----------Milvus -------------------#
    # Initialize Milvus
    vector_length = len(emb_array[0])
    milvus_helper = MilvusHelper(get_facenet_collection_name(), vector_length)

    # Insert the new vectors (emb_array) into Milvus for future queries
    vector_ids = milvus_helper.insert_vectors(emb_array)
    milvus_helper.check_vectors_inserted(vector_ids)

# -------------------------------------#

    # Add the detected faces to postgres
    engine = postgres_utils.get_engine()
    postgres_session = postgres_utils.get_session(engine)
    processed = postgres_utils.check_processed(args.source, postgres_session, models.DetectedFace)
    if not processed: # If this image isn't in the postgres db yet
        save_df_to_postgres(engine, models.DetectedFace, df)

def get_threshold_color_map():
    return {'low': {'min': 0,
                    'color': 'blue'},
            'medium': {'min': 0.4,
                       'color': 'yellow'},
            'high': {'min': 0.75,
                     'color': 'green'}}

def classify_and_threshold(classification_model, emb_array):
    # Run recognition on the detected faces (ie the classification step)
    with open(classification_model, 'rb') as infile:
        print(f'Loading classification model: {classification_model}', flush=True)
        (model, class_names) = pickle.load(infile)
    print('Loaded classifier model from file "%s"' % classification_model, flush=True)
    predictions = model.predict_proba(emb_array)
    # For each image, the index of the most-likely person
    best_class_indices = np.argmax(predictions, axis=1)
    # For each image, the probability of that most-likely person
    best_class_probabilities = predictions[
        np.arange(len(best_class_indices)),
        best_class_indices]

    # Determine the prediction name and color based on thresholds
    best_class_prediction_thresholded = list()
    threshold_color_map = get_threshold_color_map()
    colors = list()
    label_mapping = load_label_map()
    for i, prob in tqdm(enumerate(best_class_probabilities)):
        threshold = threshold_color_map['medium']['min']
        if prob > threshold:
            person_int = best_class_indices[i]
            person = list(label_mapping.keys())[list(label_mapping.values()).index(person_int)]
            best_class_prediction_thresholded.append(person)
            if prob > threshold_color_map['high']['min']:
                colors.append(threshold_color_map['high']['color'])
            else:
                colors.append(threshold_color_map['medium']['color'])
        else:
            best_class_prediction_thresholded.append('Unknown')
            colors.append(threshold_color_map['low']['color'])
    print(best_class_prediction_thresholded, flush=True)
    return best_class_prediction_thresholded, best_class_probabilities, colors


def get_postgres_row_ids_from_milvus_ids(milvus_top_k_results, num_faces, num_milvus_results, session):
    '''
    Args:
        milvus_top_k_results: The vector ids of Milvus matches
        num_faces: aka len(emb_array)
        num_milvus_results: Number of results requested from Milvus
        session: postgres session
    '''
    row_id_arrays = []
    for i in range(num_faces):
        row_ids = [None for j in range(num_milvus_results)]
        if len(milvus_top_k_results) > 0:
            k_ids = milvus_top_k_results.id_array
            if len(k_ids) > i:
                result_ids = k_ids[i]
                # For each result_id, query the postgres db for the row id
                for j in range(num_milvus_results):
                    try:
                        vector_id = result_ids[j]
                        query = session.query(models.DetectedFace.id).filter(models.DetectedFace.vector_id == vector_id)
                        result = query.first()
                        row_id = result[0]
                        row_ids[j] = row_id
                    except (IndexError,
                            TypeError):  # If Milvus has < num_milvus_results vectors, it will return a list with len < num_milvus_results; If the result from the postgres query is None, we can't take the 0th index
                        # Leave row_id as None
                        pass
        row_id_arrays.append(row_ids)
    return row_id_arrays


def extract_feature_vectors(paths):
    with tf.Graph().as_default():
        with tf.Session() as sess:
            print(f'About to load embedding extraction model', flush=True)
            fn.load_model('face/facenet/src/models/downloaded/20180408-102900/20180408-102900.pb')
            # Get input and output tensors
            images_placeholder = tf.get_default_graph().get_tensor_by_name("input:0")
            embeddings = tf.get_default_graph().get_tensor_by_name("embeddings:0")
            phase_train_placeholder = tf.get_default_graph().get_tensor_by_name("phase_train:0")
            embedding_size = embeddings.get_shape()[1]  # 512
            print(f'Embedding size: {embedding_size}', flush=True)
            # Run forward pass to calculate embeddings
            print('Calculating features for images', flush=True)
            nrof_images = len(paths)
            batch_size = 32
            nrof_batches_per_epoch = int(math.ceil(1.0 * nrof_images / batch_size))
            emb_array = np.zeros((nrof_images, embedding_size))
            image_size = 160
            for i in tqdm(range(nrof_batches_per_epoch)):
                start_index = i * batch_size
                end_index = min((i + 1) * batch_size, nrof_images)
                paths_batch = paths[start_index:end_index]
                images = fn.load_data(paths_batch, False, False, image_size)
                feed_dict = {images_placeholder: images, phase_train_placeholder: False}
                emb_array[start_index:end_index, :] = sess.run(embeddings, feed_dict=feed_dict)
    return emb_array


def load_label_map():
    # Load up the label mapping
    with open('face/facenet/label_mapping.pkl', 'rb') as handle:
        label_mapping = pickle.load(handle)
    return label_mapping

def read_mtcnn_results(bb_text_file):
    # Read in the mtcnn results txt file as a pandas dataframe
    try:
        df = pd.read_csv(bb_text_file, sep=',', header=None)
    except pd.errors.EmptyDataError:
        df = pd.DataFrame()
    return df


def check_args(args):
    c_name = get_facenet_collection_name()
    milvus_helper = MilvusHelper(c_name, None, False)
    status_has_collection, ok = milvus_helper.milvus_instance.has_collection()
    if status_has_collection.OK():
        num_vectors_in_db = milvus_helper.milvus_instance.collection_info(c_name)[1].count
    else:
        num_vectors_in_db = 0
    if args.num_milvus_results > num_vectors_in_db:
        output(f'User requested {args.num_milvus_results} matches from Milvus, but the Milvus collection only has {num_vectors_in_db}. Setting num_milvus_results to {num_vectors_in_db}.')
        args.num_milvus_results = num_vectors_in_db
    milvus_helper.close()


def parse_arguments(argv):
    print('parsing arguments...')
    parser = argparse.ArgumentParser()
    parser.add_argument('-f', '--file', type=str, help='Unaligned image.')
    parser.add_argument('-s', '--source', type=str, help='S3 input filepath')
    parser.add_argument('-n', '--num_milvus_results', type=int, default=3, help='Number of vectors Milvus query should return.')
    parser.add_argument('-m', '--model', type=str, help='Classification model.')
    parser.add_argument('-o', '--output', default='.',
                        help="Directory where result image will be written out")
    return parser.parse_args(argv)


if __name__ == '__main__':

    # Create the db and table if they don't exist. This is standardly done when the container starts, but additionally calling init_database here can be useful during development.
    postgres_utils.init_database(models.DetectedFace)

    print(f'sys argv [1:]: {sys.argv[1:]}')
    args = parse_arguments(sys.argv[1:])
    check_args(args)
    main(args)