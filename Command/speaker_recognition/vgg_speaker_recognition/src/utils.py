import librosa
import numpy as np
import ffmpeg

# ===============================================
#       code from Arsha for loading data.
# ===============================================
def load_audio(audio_or_vid_path, sr, mode='train'):
    try:
        # ffmpeg is installed, so load supports wav, mpeg, mp4, ...
        # Check that an audio stream exists
        stream_info = ffmpeg.probe(audio_or_vid_path, select_streams='a')
        streams = stream_info['streams']
        if not streams:
            return None
        # If the recording is too large, we won't be able to load it. For now, we'll just take the first three minutes and process that.
        wav, sr_ret = librosa.load(audio_or_vid_path, sr=sr, duration=180)
    except EOFError:
        msg = f'Unable to load {audio_or_vid_path}. File may be empty.'
        raise Exception(msg)
    assert sr_ret == sr
    if mode == 'train':
        extended_wav = np.append(wav, wav)
        if np.random.random() < 0.3:
            extended_wav = extended_wav[::-1]
        return extended_wav
    else:
        extended_wav = np.append(wav, wav[::-1])
        return extended_wav


def lin_spectogram_from_wav(wav, hop_length, win_length, n_fft=1024):
    linear = librosa.stft(wav, n_fft=n_fft, win_length=win_length, hop_length=hop_length) # linear spectrogram
    return linear.T


def load_data(path, win_length=400, sr=16000, hop_length=160, n_fft=512, spec_len=250, mode='train'):
    wav = load_audio(path, sr=sr, mode=mode)
    if wav is None:
        return None
    linear_spect = lin_spectogram_from_wav(wav, hop_length, win_length, n_fft)
    mag, _ = librosa.magphase(linear_spect)  # magnitude
    mag_T = mag.T
    freq, time = mag_T.shape
    if mode == 'train':
        randtime = np.random.randint(0, time-spec_len)
        spec_mag = mag_T[:, randtime:randtime+spec_len]
    else:
        spec_mag = mag_T
    # preprocessing, subtract mean, divided by time-wise var
    mu = np.mean(spec_mag, 0, keepdims=True)
    std = np.std(spec_mag, 0, keepdims=True)
    return (spec_mag - mu) / (std + 1e-5)


def get_default_params():
    # construct the data generator.
    params = {'dim': (257, None, 1),  # 257, <time variable>, 1 # Shape of input spectrogram
              'nfft': 512,
              'spec_len': 250,
              'win_length': 400,
              'hop_length': 160,
              'n_classes': 5994,
              'sampling_rate': 16000,
              'normalize': True,
              }
    return params


def predict_feature_vector(ID, feats, network_eval, params=None):
    '''
    :param ID: A wav file
    :param feats: The array we'll append results to
    '''
    print(f'Predicting feature vector for {ID}', flush=True)
    if params is None:
        params = get_default_params()
    # Load spectrogram (visual representation of frequencies over time)
    spectrograms = load_data(ID, win_length=params['win_length'], sr=params['sampling_rate'],
                                hop_length=params['hop_length'], n_fft=params['nfft'],
                                spec_len=params['spec_len'], mode='eval')
    if spectrograms is None:
        return None
    print(f'spec shape: {spectrograms.shape}', flush=True) # (257, x)
    # At this point, spectrograms (spectrogram) will have shape (dim[0], some var based on the recording length)
    spectrograms = np.expand_dims(np.expand_dims(spectrograms, 0), -1)
    print(f'spec shape: {spectrograms.shape}', flush=True) # (257, x)
    # Now spectrograms (spectrogram) will have shape (1, dim[0], some var based on the recording length, 1)
    v = network_eval.predict(spectrograms)
    # The output from the network will have shape (1, bottleneck dim)
    feats += [v]
    return feats


