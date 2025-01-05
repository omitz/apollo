# File hash

Calculate file hashes.

## Installation

Note that there's no requirement.txt (yet) for this module. Hashlib and arparse are part of the python3 standard library.

```bash
$ python -m venv venv
$ . venv/bin/activate
(venv) $ pip install --upgrade pip
```

## Usage

```bash
$ python main.py --input main.py
{'sha256': '36dfc427902b255f8a0adf021cf333375a66ae05e4cafc2eb01a0c62a7b62e3c', 'sha1': '70baff26f9823ad5e976e7927730672722b5dca3', 'md5': '6afa7974f260ab5469f53e0065d4cfd2', 'sha512': '8f06c563e8e4d87a2284da367a92371668c1392efafa7bbb9b37bad3c9a0a8bf37ae1e7f0c314c59c60581ebdd51adb3f0d1342b3f6008d8267914a2d5f152c5'}
```

## Help

```bash
$ python main.py -h
usage: main.py [-h] [-i INPUT]

optional arguments:
  -h, --help            show this help message and exit
  -i INPUT, --input INPUT
                        File to hash
```

## Alternative Docker usage

### Build

From within the hash subproject, run the docker build command and give it a tag (hash:latest in this example). This build the docker image from the Dockerfile which essentially build a python3 environment from an Ubuntu base image. The hash main.py is copied into the container.

```bash
$ docker build -t hash:latest .
```

### Usage

We run against the hashing program by volume mapping a folder on the host machine to the running container. Input file paths can now be associated with this input folder. The hash docker container launches, runs against this file, and then shuts down.

```bash
$ docker run -v /data:/data hash python main.py --input /data/dashcam/dashcam_0001.png
{'sha1': '81a6d4a1be07c0c956284138ebf3e31b1c937ea9', 'sha256': 'de6544dd65aa0d3226848bbf2074133a6d99637f8091ad66e26ff1f45fac66c5', 'sha512': '9fcf3bd8f8aa4d8b6297de6589e3b7498af787c1a26e6fe93d6fb905975093f2509f0d77e0d65546dd8c6d0ec3562dda350c211ea772806e97a100d525f707e0', 'md5': 'c4d16f68eba15b03e616195d313a8535'}
``` 
