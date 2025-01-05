#!/bin/bash
#
# Here we train the corpus.  We assume that
# prepare_directory_from_scratch.bash has already been run.
#
# TC 2019-10-10 (Thu) 
set -e

#
# 1.) Compute MFCC features.  
#
##
# Prepare a directory to save the features
featdir=/tmp/delme/
rm -rf $featdir
mkdir $featdir
##
cp ../an4/s5/conf/mfcc.conf ./conf/
# creating MFCC features:
steps/make_mfcc.sh --nj 8 --cmd "run.pl"	\
		   data/train			\
		   exp/make_mfcc/train		\
		   $featdir; 
# creating CMVN stats:
steps/compute_cmvn_stats.sh data/train exp/make_mfcc/train $featdir; 

#
# 2. Train using Monophone method.
#
## We will only train a subset of the data mainly for efficiency.
#
#utils/subset_data_dir.sh --first data/train 1000 data/train_1k
#
## train
steps/train_mono.sh --boost-silence 1.25 --nj 10 \
		    --cmd "run.pl" data/train data/lang exp/mono
# create the decode graph:
utils/mkgraph.sh data/lang exp/mono exp/mono/graph

# #
# ## align
# steps/align_si.sh --nj 16 --cmd "run.pl" \
#  		  data/train data/lang exp/mono exp/mono_ali

