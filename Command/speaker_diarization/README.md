## Speaker Diarization
Speaker diarization: partitioning an input audio stream into homogeneous segments according to the speaker identity. (Not speaker recognition.)

### Environment setup and inference

#### Option 1: Docker

Set up the following directory structure on the host machine: One directory (e.g. speaker_d) containing two directories: one containing an audio file to analyze, one for writing results to.

    ├── speaker_d
    │   ├── audio_files
    │   │   └── castro_dictators.wav
    │   └── output

Create an environment variable for the top level directory

    export SPEAKER_D=/tmp/speaker_d

 ``````
# Using CUDA-Enabled GPU
docker run --runtime=nvidia -it -u $(id -u):$(id -g) -v $SPEAKER_D:/container/ 604877064041.dkr.ecr.us-east-1.amazonaws.com/speaker_diarization:0.1.0 /bin/bash
# Without CUDA-Enabled GPU
docker run -it -u $(id -u):$(id -g) -v $SPEAKER_D:/container/ 604877064041.dkr.ecr.us-east-1.amazonaws.com/speaker_diarization:0.1.0 /bin/bash
``````

Run inference

    python speakerDiarization.py -i /container/audio_files/castro_dictators.wav -d /container/output

#### Option 2: Venv

Create a virtual environment and install requirements

    python3 -m venv <path to venv>
    pip install -r requirements.txt

Install additional requirements

    sudo apt install libasound-dev portaudio19-dev libportaudiocpp0
    pip install wheel
    pip install tensorflow==1.14.0

Run inference

    # Specify audio file (wav) to process
    export AUDIO_FILE=audio_files/castro_dictators.wav
    
    # Specify directory to write results to
    export RESULTS=output
    
    python speakerDiarization.py -i $AUDIO_FILE -d $RESULTS

### Resources

https://github.com/taylorlu/Speaker-Diarization