# Optical Character Recognition (OCR)

Given an image of word, predict what the letters are

## Environment setup and inference

### Option 1: Docker

#### Setup

On the host machine, set up the following directory structure: a directory containing two directories: one containing the word images to run ocr on, one to write output to

    ├── ocr
    │   ├── output
    │   └── word_imgs
    │       ├── 1_0.png
    │       ├── 1_10.png
    │       └── 1_20.png

Run the container, mounting the top level directory (e.g. ocr) to the container, e.g.

    export IMAGES=/tmp/ocr/
    # Using CUDA-Enabled GPU
    docker run --runtime=nvidia -it -u $(id -u):$(id -g) -v $IMAGES:/container/ 604877064041.dkr.ecr.us-east-1.amazonaws.com/ocr:0.1.0 /bin/bash
    # Without CUDA-Enabled GPU
    docker run -it -u $(id -u):$(id -g) -v $IMAGES:/container/ 604877064041.dkr.ecr.us-east-1.amazonaws.com/ocr:0.1.0 /bin/bash
    
#### Inference

    python inference.py --image_folder /container/word_imgs --outdir /container/output

### Option 2: Venv

#### Setup

Create and activate a Python 3 virtual environment

    python3 -m venv <path to virtual env>
    source <path to venv>/bin/activate

Install dependencies

    pip install -r requirements.txt
    
#### Run inference

Download the pretrained model (TPS-ResNet-BiLSTM-Attn-case-sensitive) 
https://drive.google.com/drive/folders/15WPsuPJDCzhp2SvYZLRj8mAlT3zmoAMW

```
python3 inference.py --image_folder <full path directory with word imgs> --outdir <directory to write output txt file to>
```

Results will be written to ```ocr_output.txt```.

## Resources
https://github.com/clovaai/deep-text-recognition-benchmark