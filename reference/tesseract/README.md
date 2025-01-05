# Tesseract


# Installation

## Prereqs

Ubuntu 18.04

Install the Tesseract 4.x engine.

```bash
$ sudo apt install tesseract-ocr
```

## Check version

We need tesseract >4.0 so if the following command shows less than 4, then we must build from source instead.

```bash
$ tesseract --version
```

### Build from source

Reference: 

https://tesseract-ocr.github.io/tessdoc/Compiling.html

#### Prereqs

Be sure to install libgif-dev

This wasn't in the tesseract instructions


#### Download and Install Leptonica

```bash
wget http://www.leptonica.org/source/leptonica-1.79.0.tar.gz
gunzip leptonica-1.79.0.tar.gz
tar -xvf leptonica-1.79.0.tar
cd leptonica-1.79.0
./configure
make
sudo make install
make check
```

#### Git Clone and Build Tesseract

```bash
git clone https://github.com/tesseract-ocr/tesseract.git 
cd tesseract
git checkout 4.1
./autogen.sh
./configure.sh
make
sudo make install
sudo ldconfig
```

## Download language models

From https://github.com/tesseract-ocr/tessdata, download the desired language models.

```bash
curl -JLO https://github.com/tesseract-ocr/tessdata/raw/master/eng.traineddata
curl -JLO https://github.com/tesseract-ocr/tessdata/raw/master/osd.traineddata
```

Here is the list of languages

https://tesseract-ocr.github.io/tessdoc/Data-Files

Move language models to the tesseract data folder located at /usr/local/share/tessdata/.

## Virtual Environment

Create the virtual environment

```bash
$ python3.6 -m venv --prompt tess venv
$ . venv/bin/activate
(tess) $ python -m pip install --upgrade pip wheel
(tess) $ python -m pip install -r requirements.txt
```

## Usage

```bash
(tess) $ python ocr.py -h
usage: ocr.py [-h] [-i IMAGE]

optional arguments:
  -h, --help            show this help message and exit
  -i IMAGE, --image IMAGE
```

## Data

https://www.ocrsdk.com/documentation/sample-images/

https://en.wiktionary.org/wiki/Category:Languages_by_script

## Tests

```bash
(tess) $ python -m unittest tests/test_ocr.py
```

