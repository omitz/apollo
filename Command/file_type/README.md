# File Checker

Gets the mime type for the file using magic.

## Installation

This assumes you have already cloned the Apollo repository and changed into the file_checker directory.

```bash
$ python3 -m venv venv
$ . venv/bin/activate
$ pip install --upgrade pip
$ pip install -r requirements.txt
```

## Usage
To run on a file, let's use the python module since it's available.

```bash
(venv) $ python file_checker.py --input file_checker.py
text/x-python
```

## Help 

File checker uses argparse so normal help commands are available.

```bash
$ python file_checker.py -h
usage: file_checker.py [-h] [-i INPUT]

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        File to process
```

## Unit tests

We have a couple of unit tests that can be extended at a later date when/if we run into issues.

```bash
(venv) $ python -m unittest
```

## Docker build

```bash
$ docker build -t filecheck:latest .
```

## Docker usage

```bash
$ docker run -v /data:/data filecheck python file_checker.py --input /data/dashcam/dashcam_0001.png
image/png
```

```bash
$ docker run -v /data:/data filecheck python file_checker.py --input /data/2DMOT2015.zip
application/zip
```

# Dev TODO

This module needs to be wrapper in Docker, pull from a message queue, and write results to a database.
