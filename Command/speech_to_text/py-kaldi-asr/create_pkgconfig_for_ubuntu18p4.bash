#!/bin/bash
#
# A script to compile py-kaldi-asr on Ubuntu 18.04
#
# TC 2019-10-17 (Thu)
set -e

#
# 0.) Check running from the script directory:
#
SCRIPT_DIR=$(readlink -f $(dirname $0))
RUNNING_DIR=$(readlink -f ./)
if [[ ! "$SCRIPT_DIR" == "$RUNNING_DIR" ]]; then
   echo "Must run this script from the directory where the script is located."
   exit 1
fi

## Assume  kaldi is at ../kaldi
KALDI_ROOT=$(readlink -f ../kaldi)

#
# 1.) Create data/kaldi-asr.pc:
#
cat << EOF > data/kaldi-asr.pc
kaldi_root=$KALDI_ROOT
EOF
cat << "EOF" >> data/kaldi-asr.pc
Name: kaldi-asr
Description: kaldi-asr speech recognition toolkit
Version: 5.2
Requires: lapack-atlas
Libs: -L${kaldi_root}/tools/openfst/lib -L${kaldi_root}/src/lib -lkaldi-decoder -lkaldi-lat -lkaldi-fstext -lkaldi-hmm -lkaldi-feat -lkaldi-transform -lkaldi-gmm -lkaldi-tree -lkaldi-util -lkaldi-matrix -lkaldi-base -lkaldi-nnet3 -lkaldi-online2 -lkaldi-cudamatrix -lkaldi-ivector -lfst
Cflags: -I${kaldi_root}/src  -I${kaldi_root}/tools/openfst/include
EOF


# #
# # 2.) Install
# #
# export PKG_CONFIG_PATH=$(readlink -f ./data)
# python ./setup.py install

# export LD_LIBRARY_PATH=$KALDI_ROOT/tools/openfst-1.6.7/lib/
# export LD_PRELOAD=/opt/intel/mkl/lib/intel64/libmkl_def.so:/opt/intel/mkl/lib/intel64/libmkl_avx2.so:/opt/intel/mkl/lib/intel64/libmkl_core.so:/opt/intel/mkl/lib/intel64/libmkl_intel_lp64.so:/opt/intel/mkl/lib/intel64/libmkl_intel_thread.so:/opt/intel/lib/intel64_lin/libiomp5.so
