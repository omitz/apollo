import librosa
import numpy as np
from scipy.signal import lfilter, butter

import sigproc
import constants as c


def load_wav(filename, sample_rate):
        audio, sr = librosa.load(filename, sr=sample_rate, mono=True)
        audio = audio.flatten()
        return audio


def normalize_frames (m, epsilon=1e-12):
        return np.array([(v - np.mean(v)) / max(np.std(v),epsilon)
                         for v in m])


# https://github.com/christianvazquez7/ivector/blob/master/MSRIT/rm_dc_n_dither.m
def remove_dc_and_dither(sin, sample_rate):
        if sample_rate == 16e3:
                alpha = 0.99
        elif sample_rate == 8e3:
                alpha = 0.999
        else:
                print("Sample rate must be 16kHz or 8kHz only")
                exit(1)
        sin = lfilter([1,-1], [1,-alpha], sin)
        dither = np.random.random_sample(len(sin)) + np.random.random_sample(len(sin)) - 1
        spow = np.std(dither)
        sout = sin + 1e-6 * spow * dither
        return sout


def get_fft_spectrum(filename, buckets):
        """TC 2020-02-09 (Sun) -- If we really want to have at least 400
        frames, we need to make sure the length of the signal is >
        64100, which is 4 seconds.  Look for file size > 64100 * 2 = 128200
        For 5 seconds, we need signal of lenght >= 80100
        """
        signal = load_wav(filename,c.SAMPLE_RATE)
        signal *= 2**15

        # get FFT spectrum  (frames size = NUMFRAMES by frame_len)
        signal = remove_dc_and_dither (signal, c.SAMPLE_RATE)
        signal = sigproc.preemphasis (signal, coeff=c.PREEMPHASIS_ALPHA)
        # signal = np.random.rand(64100) # TC 2020-02-09 (Sun) -- testing size

        ## Overlapping frames: number of frames = lne (signal) / frame_step
        ## eg., 77824 / 160 ~= 486
        ## Frame length = c.FRAME_LEN*c.SAMPLE_RATE = 400
        frames = sigproc.framesig (
                signal, frame_len = c.FRAME_LEN*c.SAMPLE_RATE,
                frame_step = c.FRAME_STEP*c.SAMPLE_RATE, winfunc=np.hamming)
        fft = abs(np.fft.fft(frames, n=c.NUM_FFT)) # zero-pad or trunc to 512
        fft_norm = normalize_frames(fft.T)         # transposed, each
                                                   # row is now a vecotor
                                                   
        # truncate to max bucket sizes (eg., num frames from 485 to 400)
        rsize = max(k for k in buckets if k <= fft_norm.shape[1])
        rstart = int((fft_norm.shape[1]-rsize)/2) # start from middle
        out = fft_norm[:,rstart:rstart+rsize]
        # print (out.shape)
        # 1/0
        return out
