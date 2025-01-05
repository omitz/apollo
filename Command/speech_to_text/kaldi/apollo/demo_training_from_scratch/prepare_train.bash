#!/bin/bash
#
# The goal is to prepare the minium and essential set of files to get
# a complete working ASR system.
#
# TC 2019-10-10 (Thu) 
set -e

#
# 1.) Bootstrap
#
## start from scratch:
rm -f steps utils src path.sh
rm -rf exp conf data local

## carry over the reference script files:
ln -s ../rm/s5/steps .
ln -s ../rm/s5/utils .
ln -s ../../src .

## create path.sh and update the KALDI_ROOT field:
cp ../rm/s5/path.sh .
sed --in-place '/KALDI_ROOT=/d' path.sh
#KALDI_ROOT=/share/Projects/speech-recognition/kaldi/
KALDI_ROOT=$(readlink -f ../..)
sed --in-place "1 i export KALDI_ROOT=$KALDI_ROOT" path.sh

## create data sub directories
mkdir exp
mkdir conf
mkdir -p data/train
mkdir -p data/lang
mkdir -p data/local/dict
mkdir -p data/local/lang

#
# 2.) Populate data
# 
##
# 2.1) data/train
# We must create these 4 files, one optional:
# wav.scp:
#    <utt_id> <path_to_wav_file>
#    NOTE: If <path_to_wav_file> is a shp file, then, it can be a command:
#          "sph2pipe -f wav -p -c 1 path_to_shp_file |"
#    example:
#    "fash-an251-b ~/bin/sph2pipe -f wav -p -c 1 ~/data/an251-fash-b.sph |"
# text:
#    <utt_id> <WORD1> <WORD2> <WORD3> <WORD4>
#    example:
#    "fash-cen7-b TWO SIX EIGHT FOUR FOUR ONE EIGHT"
# utt2spk:
#    <utt_id> <spkr>
#    example:
#    "fash-an251-b fash"
# spk2utt:
#    <spkr> <utt_id1> <utt_id2> <utt_id3>
#    example:
#    "fash fash-an251-b fash-an253-b fash-an254-b fash-an255-b"
# spk2gender:  [optinal]
#    <spkr> <m_or_f>
#    example:
#    "jackson_4 m"
cp ../an4/s5/data/train/wav.scp data/train/
cp ../an4/s5/data/train/text data/train/
cp ../an4/s5/data/train/utt2spk data/train/
cp ../an4/s5/data/train/spk2utt data/train/

##
# 2.2) data/local/dict/
# Must create these 4 files:
# lexicon.txt:
#    <WORD> <LEXICON>
#    example:
#    "AREA EH R IY AH"
# nonsilence_phones.txt:
#    <PHONEME>
#    example:
#    "EH_B"
# silence_phones.txt:
#    SIL
# optional_silence.txt:
#    SIL
cp ../an4/s5/data/local/dict/lexicon.txt data/local/dict/
cp ../an4/s5/data/local/dict/nonsilence_phones.txt data/local/dict/
cp ../an4/s5/data/local/dict/silence_phones.txt data/local/dict/
cp ../an4/s5/data/local/dict/optional_silence.txt  data/local/dict/

##
# 2.3) data/local/
# Create a dummy corpus.txt from data/train/text
cat data/train/text | cut -d  ' ' -f 2- > data/local/corpus.txt


#
# 3.) Prepare the language data
# 
## Run prepare_lang.sh script:
utils/prepare_lang.sh data/local/dict '<UNK>' data/local/lang data/lang 

#
# 4.) Prepare the language model
# 
## make lm.arpa
source $KALDI_ROOT/tools/env.sh
local=data/local
mkdir $local/tmp
lm_order=1 # language model order (n-gram quantity) - 1 is enough for
	   # digits grammar
ngram-count -order $lm_order				\
	    -write-vocab $local/tmp/vocab-full.txt	\
	    -wbdiscount -text $local/corpus.txt		\
	    -lm $local/tmp/lm.arpa
## make G.fst
source ./path.sh
lang=data/lang
arpa2fst --disambig-symbol=#0			\
	 --read-symbol-table=$lang/words.txt	\
	 $local/tmp/lm.arpa $lang/G.fst


