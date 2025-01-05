#!/usr/bin/env python3
#

import nemo, nemo_asr
from nemo_asr.helpers import post_process_predictions
from ruamel.yaml import YAML
from nemo.backends.pytorch.nm import DataLayerNM
from nemo.core.neural_types import NeuralType, BatchTag, TimeTag, AxisType
import torch
import scipy.io.wavfile as wave


MODEL_YAML = 'examples/asr/configs/quartznet15x5.yaml'

# TODO: update to your checkpoints
CHECKPOINT_ENCODER = 'quartznet15x5/JasperEncoder-STEP-247400.pt'
CHECKPOINT_DECODER = 'quartznet15x5/JasperDecoderForCTC-STEP-247400.pt'

# TODO: update to your audio file
AUDIO_FILE = "./input.wav"


class AudioDataLayer(DataLayerNM):
    @property
    def output_ports(self):
        return {
            "audio_signal": NeuralType({0: AxisType(BatchTag),
                                        1: AxisType(TimeTag)}),

            "a_sig_length": NeuralType({0: AxisType(BatchTag)}),
        }

    def __init__(self, **kwargs):
        DataLayerNM.__init__(self, **kwargs)
        self.output = True
        
    def __iter__(self):
        return self
    
    def __next__(self):
        if not self.output:
            raise StopIteration
        self.output = False
        return torch.as_tensor(self.signal, dtype=torch.float32), \
               torch.as_tensor(self.signal_shape, dtype=torch.int64)
        
    def set_signal(self, signal):
        self.signal = np.reshape(signal.astype(np.float32)/32768., [1, -1])
        self.signal_shape = np.expand_dims(self.signal.size, 0).astype(np.int64)
        self.output = True

    def __len__(self):
        return 1

    @property
    def dataset(self):
        return None

    @property
    def data_iterator(self):
        return self


yaml = YAML(typ="safe")
with open(MODEL_YAML) as f:
    model_definition = yaml.load(f)
labels = model_definition['labels']
model_definition['AudioToMelSpectrogramPreprocessor']['dither'] = 0

neural_factory = nemo.core.NeuralModuleFactory(
    placement=nemo.core.DeviceType.GPU,
    backend=nemo.core.Backend.PyTorch)
data_preprocessor = nemo_asr.AudioToMelSpectrogramPreprocessor(
    factory=neural_factory,
    **model_definition["AudioToMelSpectrogramPreprocessor"])
jasper_encoder = nemo_asr.JasperEncoder(
    feat_in=model_definition["AudioToMelSpectrogramPreprocessor"]["features"],
    **model_definition["JasperEncoder"])
jasper_decoder = nemo_asr.JasperDecoderForCTC(
    feat_in=model_definition["JasperEncoder"]["jasper"][-1]["filters"],
    num_classes=len(labels))
greedy_decoder = nemo_asr.GreedyCTCDecoder()

jasper_encoder.restore_from(CHECKPOINT_ENCODER)
jasper_decoder.restore_from(CHECKPOINT_DECODER)

# Instantiate necessary neural modules
data_layer = AudioDataLayer()

# Define inference DAG
audio_signal, audio_signal_len = data_layer()
processed_signal, processed_signal_len = data_preprocessor(
    input_signal=audio_signal,
    length=audio_signal_len)
encoded, encoded_len = jasper_encoder(audio_signal=processed_signal,
                                      length=processed_signal_len)
log_probs = jasper_decoder(encoder_output=encoded)
predictions = greedy_decoder(log_probs=log_probs)

_, signal = wave.read(AUDIO_FILE)
data_layer.set_signal(signal)
tensors = neural_factory.infer([predictions], verbose=False)
preds = tensors[0][0]
transcript = post_process_predictions([preds], labels)[0]

print('Transcript: "{}"'.format(transcript))
