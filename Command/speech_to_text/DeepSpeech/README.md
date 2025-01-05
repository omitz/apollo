### Speech to Text with mozilla DeepSpeech

#### Setup

Create and activate a virtualenv

    virtualenv -p python3 <path to virtual env>
    source <path to virtual env>/bin/activate
    
Install DeepSpeech

    pip3 install deepspeech

#### Run inference
Download pre-trained English model and extract

    curl -LO https://github.com/mozilla/DeepSpeech/releases/download/v0.5.1/deepspeech-0.5.1-models.tar.gz
    tar xvf deepspeech-0.5.1-models.tar.gz
    
Create an output file for results
    
    vim stt_output.txt
    :wq

Run inference and write the results to the txt file

    deepspeech --model deepspeech-0.5.1-models/output_graph.pbmm --alphabet deepspeech-0.5.1-models/alphabet.txt --lm deepspeech-0.5.1-models/lm.binary --trie deepspeech-0.5.1-models/trie --audio <path to audio file (wav format)> >> stt_output.txt


***
#### Use the Common Voice dataset

```sudo apt-get install sox libsox-fmt-mp3```

Follow instructions here: https://github.com/mozilla/DeepSpeech#common-voice-training-data
***

#### Transfer learn/run a model from a checkpoint
```git clone https://github.com/mozilla/DeepSpeech.git```

Download one of the pretrained release models from https://github.com/mozilla/DeepSpeech/releases

Follow instructions here: https://github.com/mozilla/DeepSpeech#continuing-training-from-a-release-model

Notes:
* Make sure you have the *checkpoint* model downloaded, not the standard model for inference, e.g. ```wget https://github.com/mozilla/DeepSpeech/releases/download/v0.5.1/deepspeech-0.5.1-checkpoint.tar.gz```
* Depending on the model version you want to use, you may need to switch to a different tag, e.g. ```git checkout tags/v0.5.1``` if using the 0.5.1 model
* You may need to set the ```allow_growth``` option for tensorflow gpu_options. If so, this can be done in ```util/config.py```, in ```initialize_globals```:

     ```
     # Standard session configuration that'll be used for all new sessions.
    c.session_config = tf.ConfigProto(allow_soft_placement=True, log_device_placement=FLAGS.log_placement,
                                        inter_op_parallelism_threads=FLAGS.inter_op_parallelism_threads,
                                        intra_op_parallelism_threads=FLAGS.intra_op_parallelism_threads)
    c.session_config.gpu_options.allow_growth = True
    ```

***
#### Resources
* https://github.com/mozilla/DeepSpeech
LICENSE: https://github.com/mozilla/DeepSpeech/blob/master/LICENSE

* https://voice.mozilla.org/en/datasets

