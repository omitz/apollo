import os
import sys
import argparse
from tqdm import tqdm
import numpy as np
import pandas as pd
from speaker_recognition.vgg_speaker_recognition.src.utils import get_default_params, predict_feature_vector
import speaker_recognition.vgg_speaker_recognition.src.model as model


def parse_args():
    parser = argparse.ArgumentParser()
    # set up configuration to recalculate the feature vectors of the labeled dataset and the associated files dict. Currently this should only be included when running on a local machine.
    parser.add_argument('-i', '--input', default='', help='Directory containing subdirectories where each subdirectory is the name of a person. This argument should only be passed when NOT using S3.')
    # set up training configuration.
    parser.add_argument('--gpu', default='', type=str)
    parser.add_argument('--resume', default='', type=str)
    parser.add_argument('--batch_size', default=2, type=int)
    parser.add_argument('--data_path', default='', type=str)
    # set up network configuration.
    parser.add_argument('--net', default='resnet34s', choices=['resnet34s', 'resnet34l'], type=str)
    parser.add_argument('--ghost_cluster', default=2, type=int)
    parser.add_argument('--vlad_cluster', default=8, type=int)
    parser.add_argument('--bottleneck_dim', default=512, type=int)
    parser.add_argument('--aggregation_mode', default='gvlad', choices=['avg', 'vlad', 'gvlad'], type=str)
    # set up learning rate, training loss and optimizer.
    parser.add_argument('--loss', default='softmax', choices=['softmax', 'amsoftmax'], type=str)
    parser.add_argument('--test_type', default='normal', choices=['normal', 'hard', 'extend'], type=str)
    args = parser.parse_args()
    return args


def build_dict():
    input_dir = '/tmp/vips'
    # Build dict (ultimately will be a dataframe) to track confidence scores
    known_people = [item for item in os.listdir(input_dir) if os.path.isdir(os.path.join(input_dir, item))]
    files = {'person': [], 'wav': []}
    for person in known_people:
        person_dir = os.path.join(input_dir, person)
        wavfiles = os.listdir(person_dir)
        for wav in wavfiles:
            files['person'].append(person)
            files['wav'].append(os.path.join(person_dir, wav))
    return files


def inference(local_file_path):
    '''
    :return: results_dict: A dictionary with two keys: dataframe and embedding_array
    '''
    params = get_default_params()

    # Load up the Keras model
    args = parse_args()
    network_eval = model.vggvox_resnet2d_icassp(input_dim=params['dim'],
                                                num_class=params['n_classes'],
                                                mode='eval', args=args)
    print('Loading model weights', flush=True)
    network_eval.load_weights('speaker_recognition/vgg_speaker_recognition/model/resnet34_vlad8_ghost2_bdim512_deploy/weights.h5')

    # Get the feature vectors of the existing dataset.
    # Note: This assumes we will always be comparing against the same, static dataset of voice recordings.
    # If new, labeled voice recordings are added to the dataset, feats.npy and files_dict.csv will need to be deleted.
    # Then, this block should be rerun using a CUDA-enabled GPU, then the new feats and files_dict files will need to be saved to the new docker image.
    feats_path = 'speaker_recognition/vgg_speaker_recognition/feats.npy'
    files_dict_path = 'speaker_recognition/vgg_speaker_recognition/files_dict.csv'
    if not os.path.exists(feats_path) or not os.path.exists(files_dict_path):
        files_dict = build_dict(args)
        feats = []
        print("Predicting feature vectors")
        for wavfile in tqdm(files_dict['wav']):
            feats = predict_feature_vector(wavfile, feats, network_eval, params)
        feats = np.array(feats)
        # The shape of feats is (num samples, 1, 512) (512 being the bottleneck dim)
        np.save(feats_path, feats)
        # We'll save the files dict as well, since those files need to be in the same order as the feats array
        files_df = pd.DataFrame.from_dict(files_dict)
        files_df.to_csv(files_dict_path, index=False)
    else:
        feats = np.load(feats_path)
        files_dict = load_files_dict(files_dict_path)

    # Get the feature vector of the new recording
    query_feat = predict_feature_vector(local_file_path, [], network_eval, params)
    if query_feat is None:
        return {'dataframe': None, 'embedding_array': None}
    unknown_vector = query_feat[0][0]

    print('Comparing query sample against old samples...', flush=True)
    files_dict['score'] = []

    # Compare the new sample against each of the old samples
    for i, feat in enumerate(feats):
        known_vector = feat[0]
        # Compute pair-wise similarity
        score = np.sum(known_vector * unknown_vector)
        files_dict['score'].append(score)

    # Reformat predictions as a dataframe
    df = pd.DataFrame.from_dict(files_dict)
    # Instead of saving out the comparison against each audio file, save out only the highest-score result
    best_match_index = np.argmax(np.array(df.score.values))
    df = df.iloc[[best_match_index]]
    # df to csv eg:
    # person,wav,score
    # Ewan_McGregor,/tmp/speaker_recog/audio_files/Ewan_McGregor/00004.wav,0.9570140838623047

    results_dict = {'dataframe': df, 'embedding_array': unknown_vector}
    return results_dict


def load_files_dict(files_dict_path):
    files_df = pd.read_csv(files_dict_path)
    files_dict = files_df.to_dict(orient='list')
    return files_dict
