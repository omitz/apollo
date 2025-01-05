# Kaldi Speech Recognition Toolkit (C++ Version)

Convert speech to text.  Kaldi is needed by py-kaldi-asr.

## Method 1: Installing from Source
### Environment Setup

- Assuming a minimal Ubuntu 18.04 with:
  - 4 CPUs
  - 4 GB memory
  - 20 GB disk

- Add dependent packages:
```bash
sudo apt -y install make
sudo apt -y install make-guile
sudo apt -y install build-essential
sudo apt -y install zlib1g-dev automake autoconf sox 
sudo apt -y install gfortran libtool subversion python
```


### Code Compilation (Overview)

```bash
cd tools/
extras/install_mkl.sh        # takes about 4 minutes [0]
extras/check_dependencies.sh # all dependencies satisfied
make -j 4                    # takes about 4 minutes [1]
make                         # making sure everything compiled O.K.
extras/install_openblas.sh   # takes a while
extras/install_irstlm.sh     # takes about 1 minutes
cd ../src
./configure --shared         # [2]
make -j clean depend
make -j 4                    # If run out of memory, just run again
make                         # making sure everything compiled O.K.
```

- [0] MKL is for Intel CPUs only.  We can install OpenBlas instead:
  - `./extras/install_openblas.sh`
- [1] There are a few dependencies.
  - sph2pipe
  - cub
  - sctk
  - openfst
- [2] If we are not using MKL or we are using Arm CPU,
  - env CXXFLAGS="-O3" ./configure --shared --mathlib=OPENBLAS 
  - cat kaldi.mk

### Code Compilation For Android Linux (Added on 2019-12-27)
We are going to compile Kalid for Android Linux.  

#### 0.) Prerequisite 
The gernal outline is as follows:
  1. On your Android device, first need to install FDroid, and then,
    install Termux and AnLinux.
  2. From AnLinux, install Debian or Ubuntu (Debian is prefered
     because it uses Python 3.7 which is needed for Tensorflow).
  3. Once in Debian/Ubuntu Linux, install the following packages:
	 ```bash
apt-get install -y g++ make automake autoconf bzip2 unzip sox gfortran libtool
apt-get install -y subversion python2.7 python python3 zlib1g-dev git
apt-get install -y command-not-found gawk 
	 ```


#### 1.) Check Kaldi Dependencies
```bash
cd tools/
extras/check_dependencies.sh # all dependencies satisfied
```

#### 2.) Install OpenBlas (30 minutes) 

Edit `./extras/install_openblas.sh` installer script to change the last line.  Before:

`make PREFIX=$(pwd)/OpenBLAS/install USE_LOCKING=1 USE_THREAD=0 -C OpenBLAS all install`

After:

`make PREFIX=$(pwd)/OpenBLAS/install USE_LOCKING=1 USE_THREAD=0  TARGET=ARMV8 -C OpenBLAS all install -j 4`

Now, run the modified installer script:
```bash
cd tools/
./extras/install_openblas.sh
```

#### 3.) Install Tools (25 minutes)
```bash
cd tools/
env CXXFLAGS="-O3" make -j 4
```

#### 3.) Install irstlm (5 minutes)
Now, modiy `extras/install_irstlm.sh` to change the line from:

`make; make install`

to:

`make -j 4; make install`

```bash
cd tools/
./extras/install_irstlm.sh
```

#### 4.) Install Kaldi (30+ minutes) 
```
cd src
env CXXFLAGS="-O3" ./configure --shared --mathlib=OPENBLAS 
make -j 4 clean depend
make -j 4                    # If run out of memory, just run again
make                         # making sure everything compiled O.K.
```
If compilation crashed, it's due to low memory.  Try running with fewer cores: 

`make -j 2`


#### 5.) Install liblbfgs (2 minutes)
Edit `extras/install_liblbfgs.sh`, change lines from:
```
cd liblbfgs-$VER
./configure --prefix=`pwd`
make
```
to:
```
cd liblbfgs-$VER
wget -O config.guess 'https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.guess;hb=HEAD'
wget -O config.sub 'https://git.savannah.gnu.org/gitweb/?p=config.git;a=blob_plain;f=config.sub;hb=HEAD'
./configure --prefix=`pwd` --build=aarch64-linux-gnu
make -j 4
```

Now, we are ready to compile. 
```
cd tools/
extras/install_liblbfgs.sh
```



#### 6.) Install srilm (20 minutes) 

Edit `./extras/install_irstlm.sh`, change the lines from:

```
mtype=`sbin/machine-type`
```
to:

```
mtype=aarch64
export MACHINE_TYPE=aarch64
echo '
   # Use the GNU C compiler.
   GCC_FLAGS = -march=armv8-a -Wall -Wno-unused-variable -Wno-uninitialized -DNO_ZIO -DNO_ICONV
   CC = $(GCC_PATH)gcc $(GCC_FLAGS) -Wimplicit-int
   CXX = $(GCC_PATH)g++ $(GCC_FLAGS) -DINSTANTIATE_TEMPLATES

   OPTIMIZE_FLAGS = -g -O2 -Os -DNDEBUG -ffunction-sections -funwind-tables -fno-short-enums
   DEBUG_FLAGS = -g -DDEBUG
   PROFILE_FLAGS = -g -pg -O3

   # Optional linking flags.
   EXPORT_LDFLAGS = -s

   # Shared compilation flags.
   CFLAGS = $(ADDITIONAL_CFLAGS) $(INCLUDES)
   CXXFLAGS = $(ADDITIONAL_CXXFLAGS) $(INCLUDES)

   # Shared linking flags.
   LDFLAGS = $(ADDITIONAL_LDFLAGS) -L$(SRILM_LIBDIR)

   # Other useful compilation flags.
   ADDITIONAL_CFLAGS = -fopenmp
   ADDITIONAL_CXXFLAGS = -fopenmp
# ADDITIONAL_CFLAGS = -DNEED_RAND48 -DNO_TLS
# ADDITIONAL_CXXFLAGS = -DNEED_RAND48 -DNO_TLS

   # Other useful include directories.
   ADDITIONAL_INCLUDES = 

   # Other useful linking flags.
   ADDITIONAL_LDFLAGS = 

   # Other useful libraries.
   SYS_LIBRARIES = -lpthread

   # run-time linker path flag
   RLD_FLAG = -R

   # Tcl support (standard in Linux)
   TCL_INCLUDE = 
   TCL_LIBRARY =

   NO_TCL = 1

   # No ranlib
   RANLIB = :

   # Generate dependencies from source files.
   GEN_DEP = $(CC) $(CFLAGS) -MM

   GEN_DEP.cc = $(CXX) $(CXXFLAGS) -MM

   # Run lint.
   LINT = lint
   LINT_FLAGS = -DDEBUG $(CFLAGS)

   # Location of gawk binary
   GAWK = /usr/bin/awk

   # Location of perl binary
   PERL = /usr/bin/perl
   ' > common/Makefile.machine.$mtype
```
Also from :

    make || exit 1

to:

    make -j 4 || exit 1  ## don't use make -j




Now we are ready to compile:

```
cd tools/
./extras/install_irstlm.sh
```


### Demo and Examples

#### Train Model from Scratch

  See [apollo/demo_training_from_scratch/](apollo/demo_training_from_scratch/)
  

#### Use Pre-trained Model

  See [apollo/demo_using_pretrained_model/](apollo/demo_using_pretrained_model/)
  

## Method 2: Create a Docker Image
### Wihtout GPI
```bash
cd docker/debian9.8-cpu/
docker build -t apollo/kaldi-cpp .
```

### With GPU
```bash
cd docker/ubuntu16.04-gpu/
docker build -t apollo/kaldi-cpp .
```

## Current Status and Random Thoughts:
 - Kaldi C++ version is meant to be used by speech researchers.
 - The repository does not include any pre-trained models but it has
   many scripts in the egs/ subdirectory.  Each script assumes a
   particular dataset already downloaded.  Majority of the dataset are
   proprietary.  Some datasets are free howerver.
 - One can also create and train a model with own/custom dataset.
 - A dataset needs to be annotated not only with transcription, but
   also the pronouciation (eg., phonemes).
