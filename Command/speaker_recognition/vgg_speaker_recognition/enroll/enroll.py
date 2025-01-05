import pandas as pd

from speaker_recognition.vgg_speaker_recognition.inference import *

params = get_default_params()

# Load up the Keras model
args = parse_args()
network_eval = model.vggvox_resnet2d_icassp(input_dim=params['dim'],
                                            num_class=params['n_classes'],
                                            mode='eval', args=args)
print('Loading model weights', flush=True)
network_eval.load_weights(
    '../model/resnet34_vlad8_ghost2_bdim512_deploy/weights.h5')

feats_path = '../feats.npy'
files_dict_path = '../files_dict.csv'

files_dict = build_dict()
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