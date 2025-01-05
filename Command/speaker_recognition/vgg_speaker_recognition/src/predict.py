from __future__ import absolute_import
from __future__ import print_function
import os
import sys
import numpy as np

sys.path.append('../tool')
import toolkits
import utils as ut

import pdb
# ===========================================
#        Parse the argument
# ===========================================
import argparse
parser = argparse.ArgumentParser()
# set up training configuration.
parser.add_argument('--gpu', default='', type=str)
parser.add_argument('--resume', default='', type=str)
parser.add_argument('--batch_size', default=16, type=int)
parser.add_argument('--data_path', default='/media/weidi/2TB-2/datasets/voxceleb1/wav', type=str)
# set up network configuration.
parser.add_argument('--net', default='resnet34s', choices=['resnet34s', 'resnet34l'], type=str)
parser.add_argument('--ghost_cluster', default=2, type=int)
parser.add_argument('--vlad_cluster', default=8, type=int)
parser.add_argument('--bottleneck_dim', default=512, type=int)
parser.add_argument('--aggregation_mode', default='gvlad', choices=['avg', 'vlad', 'gvlad'], type=str)
# set up learning rate, training loss and optimizer.
parser.add_argument('--loss', default='softmax', choices=['softmax', 'amsoftmax'], type=str)
parser.add_argument('--test_type', default='normal', choices=['normal', 'hard', 'extend'], type=str)

global args
args = parser.parse_args()

def main():

    # gpu configuration
    toolkits.initialize_GPU(args)

    import model
    # ==================================
    #       Get Train/Val.
    # ==================================
    print('==> calculating test({}) data lists...'.format(args.test_type))

    # verify_list is a list where each row is
    # <same or not>, <recording of person x>, <if same, recording of person x; else, recording of person y>
    if args.test_type == 'normal':
        verify_list = np.loadtxt('../meta/voxceleb1_veri_test.txt', str)
    elif args.test_type == 'hard':
        verify_list = np.loadtxt('../meta/voxceleb1_veri_test_hard.txt', str)
    elif args.test_type == 'extend':
        verify_list = np.loadtxt('../meta/voxceleb1_veri_test_extended.txt', str)
    else:
        raise IOError('==> unknown test type.')

    verify_label = np.array([int(i[0]) for i in verify_list])
    list1 = np.array([os.path.join(args.data_path, i[1]) for i in verify_list])
    list2 = np.array([os.path.join(args.data_path, i[2]) for i in verify_list])

    total_list = np.concatenate((list1, list2))
    unique_wavfile_list = np.unique(total_list)

    # ==================================
    #       Get Model
    # ==================================
    params = ut.get_default_params()

    # Load up the Keras model
    network_eval = model.vggvox_resnet2d_icassp(input_dim=params['dim'],
                                                num_class=params['n_classes'],
                                                mode='eval', args=args)

    # ==> load pre-trained model ???
    if args.resume:
        # ==> get real_model from arguments input,
        # load the model if the imag_model == real_model.
        if os.path.isfile(args.resume):
            network_eval.load_weights(os.path.join(args.resume), by_name=True)
            result_path = set_result_path(args)
            print('==> successfully loading model {}.'.format(args.resume))
        else:
            raise IOError("==> no checkpoint found at '{}'".format(args.resume))
    else:
        raise IOError('==> please type in the model to load')

    print('==> start testing.')

    # The feature extraction process has to be done sample-by-sample,
    # because each sample is of different lengths.
    total_length = len(unique_wavfile_list)
    feats, scores, labels = [], [], []
    for c, ID in enumerate(unique_wavfile_list):
        if c % 50 == 0: print('Finish extracting features for {}/{}th wav.'.format(c, total_length))

        feats = ut.predict_feature_vector(ID, feats, network_eval, params)

    feats = np.array(feats)

    # ==> compute the pair-wise similarity.
    for c, (p1, p2) in enumerate(zip(list1, list2)):
        # Get the index in unique_wavfile_list for each person
        ind1 = np.where(unique_wavfile_list == p1)[0][0]
        ind2 = np.where(unique_wavfile_list == p2)[0][0]

        # Get the feature vector for each person
        v1 = feats[ind1, 0]
        v2 = feats[ind2, 0]

        scores += [np.sum(v1*v2)]
        labels += [verify_label[c]]
        print('scores : {}, gt : {}'.format(scores[-1], verify_label[c]))

    scores = np.array(scores)
    labels = np.array(labels)

    np.save(os.path.join(result_path, 'prediction_scores.npy'), scores)
    np.save(os.path.join(result_path, 'groundtruth_labels.npy'), labels)

    eer, thresh = toolkits.calculate_eer(labels, scores)
    print('==> model : {}, EER: {}'.format(args.resume, eer))


def set_result_path(args):
    model_path = args.resume
    exp_path = model_path.split(os.sep)
    result_path = os.path.join('../result', exp_path[2], exp_path[3])
    if not os.path.exists(result_path): os.makedirs(result_path)
    return result_path


if __name__ == "__main__":
    main()
