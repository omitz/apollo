

## Enrolling a new set of VIPS

For this analytic, we can provide the ability to classify new people without having to retraing a model. We only need to create their enrollment data (feature vectors and labels). (To retrain the model that outputs the feature vectors, reference the open-source repository: https://github.com/WeidiXie/VGG-Speaker-Recognition)

Create a VIPs dataset named `vips`
    The vips directory should have the following structure: each subdirectory is the label for the person, and each file in that subdirectory is an audio recording of that person:
    
           ├── vips
           │   ├── John_Smith
           │   │   ├── 1.wav
           │   │   ├── 2.wav
           │   │   ├── 3.wav
           │   ├── Jane_Doe
           │   │   └── 1.wav
           |   |   (etc...)
           
   Each recording should have only one voice in it. It's recommended to have at least 10 recordings for each person. 
   Save the `vips` directory to `/tmp/`.
   
   
### Prerequisites

A Python 3.6 environment (`venv` is recommended).


### Train the classification model

    # From Command/speaker_recognition/vgg_speaker_recognition/retrain
    sh retrain.sh
    
The new `feats.npy` and `files_dict.csv` (which are referenced at inference time) will overwrite the old versions. 