"""A demo script showing how to DIARIZATION ON WAV USING UIS-RNN."""

import numpy as np
import uisrnn
import librosa
import sys

sys.path.append('ghostvlad')
sys.path.append('visualization')
import toolkits
import model as spkModel
import os
from viewer import PlotDiar
import soundfile as sf
import argparse


SAVED_MODEL_NAME = 'pretrained/saved_model.uisrnn_benchmark'


def append2dict(speakerSlice, spk_period):
    key = list(spk_period.keys())[0]
    value = list(spk_period.values())[0]
    timeDict = {}
    timeDict['start'] = int(value[0] + 0.5)
    timeDict['stop'] = int(value[1] + 0.5)
    if (key in speakerSlice):
        speakerSlice[key].append(timeDict)
    else:
        speakerSlice[key] = [timeDict]

    return speakerSlice


def arrangeResult(labels,
                  time_spec_rate):  # {'1': [{'start':10, 'stop':20}, {'start':30, 'stop':40}], '2': [{'start':90, 'stop':100}]}
    '''
	Go from label-encoded model prediction (e.g. [0, 0, 1, 0, 2, 2]) to dict
	'''
    lastLabel = labels[0]
    speakerSlice = {}
    j = 0
    for i, label in enumerate(labels):
        if (label == lastLabel):
            continue
        speakerSlice = append2dict(speakerSlice, {lastLabel: (time_spec_rate * j, time_spec_rate * i)})
        j = i
        lastLabel = label
    speakerSlice = append2dict(speakerSlice, {lastLabel: (time_spec_rate * j, time_spec_rate * (len(labels)))})
    return speakerSlice


def genMap(intervals):  # interval slices to maptable
    slicelen = [sliced[1] - sliced[0] for sliced in intervals.tolist()]
    mapTable = {}  # vad erased time to origin time, only split points
    idx = 0
    for i, sliced in enumerate(intervals.tolist()):
        mapTable[idx] = sliced[0]
        idx += slicelen[i]
    mapTable[sum(slicelen)] = intervals[-1, -1]

    keys = [k for k, _ in mapTable.items()]
    keys.sort()
    return mapTable, keys


def fmtTime(timeInMillisecond):
    millisecond = timeInMillisecond % 1000
    minute = timeInMillisecond // 1000 // 60
    second = (timeInMillisecond - minute * 60 * 1000) // 1000
    time = '{}:{:02d}.{}'.format(minute, second, millisecond)
    return time


def load_wav(vid_path, sr):
    wav, _ = librosa.load(vid_path, sr=sr)
    intervals = librosa.effects.split(wav.copy(), top_db=20)
    wav_output = []
    for sliced in intervals:
        wav_output.extend(wav[sliced[0]:sliced[1]])
    return wav, np.array(wav_output), (intervals / sr * 1000).astype(int)


def lin_spectogram_from_wav(wav, hop_length, win_length, n_fft=1024):
    linear = librosa.stft(wav, n_fft=n_fft, win_length=win_length, hop_length=hop_length)  # linear spectrogram
    return linear.T


# 0s        1s        2s                  4s                  6s
# |-------------------|-------------------|-------------------|
# |-------------------|
#           |-------------------|
#                     |-------------------|
#                               |-------------------|
def load_data(path, win_length=400, sr=16000, hop_length=160, n_fft=512, embedding_per_second=0.5, overlap_rate=0.5):
    '''
	:param sr: Sampling rate. Most speech corpora are 16kHz, i.e. 16,000 samples per second
	'''
    original_wav, wav, intervals = load_wav(path, sr=sr)
    linear_spect = lin_spectogram_from_wav(wav, hop_length, win_length, n_fft)
    mag, _ = librosa.magphase(linear_spect)  # magnitude
    mag_T = mag.T
    freq, time = mag_T.shape
    spec_mag = mag_T

    spec_len = sr / hop_length / embedding_per_second
    spec_hop_len = spec_len * (1 - overlap_rate)

    cur_slide = 0.0
    utterances_spec = []

    while (True):  # slide window.
        if (cur_slide + spec_len > time):
            break
        spec_mag = mag_T[:, int(cur_slide + 0.5): int(cur_slide + spec_len + 0.5)]

        # preprocessing, subtract mean, divided by time-wise var
        mu = np.mean(spec_mag, 0, keepdims=True)
        std = np.std(spec_mag, 0, keepdims=True)
        spec_mag = (spec_mag - mu) / (std + 1e-5)
        utterances_spec.append(spec_mag)

        cur_slide += spec_hop_len

    return original_wav, utterances_spec, intervals

def parse_args():
    parser = argparse.ArgumentParser()
    # args for inference
    parser.add_argument('-i', '--input', help="File to process")
    parser.add_argument('-d', '--outdir', help="Directory to save results to")
    parser.add_argument('-v', '--visualize', action='store_true', help="Plot the speakers over the course of the input file. Do not use in docker container.")

    # set up training configuration.
    parser.add_argument('--gpu', default='', type=str)
    parser.add_argument('--resume', default=r'ghostvlad/pretrained/weights.h5', type=str)
    parser.add_argument('--data_path', default='4persons', type=str)
    # set up network configuration.
    parser.add_argument('--net', default='resnet34s', choices=['resnet34s', 'resnet34l'], type=str)
    parser.add_argument('--ghost_cluster', default=2, type=int)
    parser.add_argument('--vlad_cluster', default=8, type=int)
    parser.add_argument('--bottleneck_dim', default=512, type=int)
    parser.add_argument('--aggregation_mode', default='gvlad', choices=['avg', 'vlad', 'gvlad'], type=str)
    # set up learning rate, training loss and optimizer.
    parser.add_argument('--loss', default='softmax', choices=['softmax', 'amsoftmax'], type=str)
    parser.add_argument('--test_type', default='normal', choices=['normal', 'hard', 'extend'], type=str)

    return parser.parse_args()


def main():
    args = parse_args()
    wav_path = args.input
    outdir = args.outdir

    embedding_per_second = 1.0
    overlap_rate = 0.5

    # gpu configuration
    toolkits.initialize_GPU(args)

    params = {'dim': (257, None, 1),
              'nfft': 512,
              'spec_len': 250,
              'win_length': 400,
              'hop_length': 160,
              'n_classes': 5994,
              'sampling_rate': 16000,
              'normalize': True,
              }

    network_eval = spkModel.vggvox_resnet2d_icassp(input_dim=params['dim'],
                                                   num_class=params['n_classes'],
                                                   mode='eval', args=args)
    network_eval.load_weights(args.resume, by_name=True)

    model_args, _, inference_args = uisrnn.parse_arguments()
    model_args.observation_dim = 512
    uisrnnModel = uisrnn.UISRNN(model_args)
    uisrnnModel.load(SAVED_MODEL_NAME)

    original_wav, specs, intervals = load_data(wav_path, embedding_per_second=embedding_per_second,
                                               overlap_rate=overlap_rate)
    mapTable, keys = genMap(intervals)

    feats = []
    for spec in specs:
        spec = np.expand_dims(np.expand_dims(spec, 0), -1)
        v = network_eval.predict(spec)
        feats += [v]

    feats = np.array(feats)[:, 0, :].astype(float)  # [splits, embedding dim]

    # For each time bin, which speaker is speaking
    predicted_label = uisrnnModel.predict(feats, inference_args)

    x = 1000
    time_spec_rate = x * (1.0 / embedding_per_second) * (1.0 - overlap_rate)  # speaker embedding every ?ms
    center_duration = int(x * (1.0 / embedding_per_second) // 2)
    speakerSlice = arrangeResult(predicted_label, time_spec_rate)

    for speaker, timeDicts in speakerSlice.items():  # time map to orgin wav(contains mute)
        for tid, timeDict in enumerate(timeDicts):
            s = 0
            e = 0
            for i, key in enumerate(keys):
                if (s != 0 and e != 0):
                    break
                if (s == 0 and key > timeDict['start']):
                    offset = timeDict['start'] - keys[i - 1]
                    s = mapTable[keys[i - 1]] + offset
                if (e == 0 and key > timeDict['stop']):
                    offset = timeDict['stop'] - keys[i - 1]
                    e = mapTable[keys[i - 1]] + offset

            speakerSlice[speaker][tid]['start'] = s
            speakerSlice[speaker][tid]['stop'] = e

    txt_path = os.path.join(outdir, 'speaker_diarization_output.txt')
    file = open(txt_path, 'a+')
    file.write('file,speaker,start_time,stop_time\n')
    for speaker, timeDicts in speakerSlice.items():
        print('========= ' + str(speaker) + ' =========')
        for timeDict in timeDicts:
            s = timeDict['start']
            e = timeDict['stop']

            # Save out the slice as a wav file # TODO This method might overly crop the wav
            s_khz = s * params['sampling_rate'] / x
            e_khz = e * params['sampling_rate'] / x
            slice = original_wav[int(s_khz):int(e_khz)]
            basename, _ = os.path.splitext(os.path.basename(wav_path))

            outpath = f'{basename}_{speaker}_{s}.wav'
            outpath = os.path.join(outdir, outpath)
            sf.write(outpath, slice, params['sampling_rate'])

            s = fmtTime(s)  # change point moves to the center of the slice
            e = fmtTime(e)
            print(s + ' ==> ' + e)
            file.write(f'{wav_path},{speaker},{s},{e}\n')

    if args.visualize:
        p = PlotDiar(map=speakerSlice, wav=wav_path, gui=True, size=(25, 6))
        p.draw()
        p.plot.show()


if __name__ == '__main__':
    main()
