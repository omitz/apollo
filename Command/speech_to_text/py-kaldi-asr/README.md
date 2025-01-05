# py-kaldi-asr -- Kaldi Speech Recognition Toolkit (Python Wrapper)

Some simple wrappers around kaldi-asr intended to make using kaldi's
online nnet3-chain decoders as convenient as possible. Kaldi's online
GMM decoders are also supported.

Target audience are developers who would like to use kaldi-asr as-is
for speech recognition in their application on GNU/Linux operating
systems.

## Environment Setup

### Assuming a minimal Ubuntu 18.04 with:
  - 4 CPUs
  - 4 GB memory
  - 20 GB disk

###  kaldi must be already installed.
  - For instructions on how to install kaldi, see [../kaldi/](../kaldi/).

###  Add dependent packages:

```bash
sudo apt -y install libatlas-base-dev
sudo apt -y install pkg-config
sudo apt -y install python-dev
sudo apt -y install python3-dev
sudo apt -y install virtualenv
```

###  Setup python virtualenv:
#### For Python 2, do:
```bash
virtualenv py-kaldi-asr-env
source py-kaldi-asr-env/bin/activate
```

#### For Python 3, do:
```bash
virtualenv -p python3 py-kaldi-asr-env
source py-kaldi-asr-env/bin/activate
```

#### Method 1: Install latest python packages

```bash
(py-kaldi-asr-env) $: (
  pip install numpy
  pip install cython
  pip install ipython
  pip install future
)
```

#### Method 2: Install from frozen requirements.txt
##### Python 2
```bash
(py-kaldi-asr-env) $: pip install -r requirements.txt
```

##### Python 3
```bash
(py-kaldi-asr-env) $: pip install -r requirements3.txt
```


###  Compile and Install:
```bash
source py-kaldi-asr-env/bin/activate
(py-kaldi-asr-env) $: (
  ./create_pkgconfig_for_ubuntu18p4.bash
  export PKG_CONFIG_PATH=$(readlink -f ./data)
  python ./setup.py install
  pip install py-nltools
)
```

## Demo and Examples

### Download a Pre-trained Model
```bash
cd demo
wget https://goofy.zamia.org/zamia-speech/asr-models/kaldi-generic-en-tdnn_f-r20190609.tar.xz
tar xvf kaldi-generic-en-tdnn_f-r20190609.tar.xz
ln -s kaldi-generic-en-tdnn_f-r20190609 kaldi-generic-en-tdnn_f
```


### Setup Environment Variables
```bash
source py-kaldi-asr-env/bin/activate
(py-kaldi-asr-env) $: (
  KALDI_ROOT=$(readlink -f ../kaldi)
  export LD_LIBRARY_PATH=$KALDI_ROOT/tools/openfst-1.6.7/lib/:$KALDI_ROOT/src/lib/
  MKL_ROOT=/opt/intel/mkl/lib/intel64
  INTL_ROOT=/opt/intel/lib/intel64_lin/
  export LD_PRELOAD=$MKL_ROOT/libmkl_def.so:$MKL_ROOT/libmkl_avx2.so:$MKL_ROOT/libmkl_core.so:$MKL_ROOT/libmkl_intel_lp64.so:$MKL_ROOT/libmkl_intel_thread.so:$INTL_ROOT/libiomp5.so
)
```


### Run Demo

Before you can run the demo programs, you need to set the above
environment variables (i.e., LD_PRELOAD and LD_LIBRARY_PATH).

#### Python 2
```bash
(py-kaldi-asr-env) $: (
  cd demo
  python kaldi_decode_wav.py --model-dir ./kaldi-generic-en-tdnn_f/ demo1.wav
)
```

#### Python 3
```bash
(py-kaldi-asr-env) $: (
  cd demo
  python3 kaldi_decode_wav3.py --model-dir ./kaldi-generic-en-tdnn_f/ demo1.wav
)
```

## Current Status and Random Thoughts:
 - Kaldi Python version is much easier to use for people who are not
   speech researchers.
 - The repository does not include any pre-trained models but
   instruction for downloading pre-trained models are provided.
 - Assuems C++ Kaldi libraries and executables are already
   pre-installed.  The Python wrappers invoke the pre-installed Kalid
   C++ libraries and executables.
