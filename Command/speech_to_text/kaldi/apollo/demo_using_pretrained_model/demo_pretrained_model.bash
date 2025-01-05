#!/bin/bash
#
# This script shows how to use a pre-trained model to decode an audio.
#
#  Assumptions:
#   1.) Kaldi has been installed
#   2.) Download the ASpIRE Chain Model
#         http://kaldi-asr.org/models/1/0001_aspire_chain_model.tar.gz
#      and unzip it and put it in:
#         /share/Projects/speech-recognition/pre-trained-models/ASpIRE/
#   3.) Download the test voice file:
#        https://drive.google.com/file/d/1D6eGYK4dCzLend4osd33lXe7x9YjTrgl/view
#      and process it by:
#        sox -t wav <IN_WAV_FILE> -c 1 -b 16 -r 8000 -t wav - > <OUT_WAV_FILE>
#      and rename <OUT_WAV_FILE> as :
#        ~/Downloads/kaldi-data-and-models/testme_8Khz_1ch_16bit.wav
#
# ref: https://tinyurl.com/y24b6lpw
# TC 2019-10-11 (Fri) --
#
set -e

#
# 1.) make a s5 directory.  The directory structure must be the
# following format:
#    <path>/kaldi/egs/<projName>/s5
#
# Assumeing we are at <path>/kaldi/egs/<projName>:
rm -rf s5
mkdir s5


#
# 2.) Create symoblic links:
# 
## From the aspire exmaple:
cd s5
ln -s ../../aspire/s5/conf .
ln -s ../../aspire/s5/utils .
ln -s ../../aspire/s5/steps .
ln -s ../../aspire/s5/path.sh .

## From the downloaded model:
### Please change the line below accordingly
ln -s /share/Projects/speech-recognition/pre-trained-models/ASpIRE/exp .

#
# 3.) Create the decoder graph is needed.  Takes about 6 minutes.
#
# NOTE: if you move the model directory, you have to recmpute the
# model even if you readjust the symbolic link.
#
if [[ ! -e exp/tdnn_7b_chain_online/final.mdl ]]; then
    echo "Need to create deocder graph"
    ln -s /share/Projects/speech-recognition/pre-trained-models/ASpIRE/data .

    ## prepare the conf files
    steps/online/nnet3/prepare_online_decoding.sh	\
	--mfcc-config conf/mfcc_hires.conf		\
	data/lang_chain exp/nnet3/extractor		\
	exp/chain/tdnn_7b exp/tdnn_7b_chain_online

    ## create the final.mdl
    utils/mkgraph.sh --self-loop-scale 1.0 data/lang_pp_test \
		     exp/tdnn_7b_chain_online \
		     exp/tdnn_7b_chain_online/graph_pp
    
    ## done, don't need data anymore
    rm data
fi


#
# 4.) Create a test script that will decode a test voice file.
# 
WAV_FILE=~/Downloads/kaldi-data-and-models/testme_8Khz_1ch_16bit.wav
cat << EOF > test.bash
#!/bin/bash

rm -rf exp/tdnn_7b_chain_online/decode_out/

#
# Prepare a test audio
#
mkdir -p test_data
echo "utterId spkId" > test_data/utt2spk
echo "utterId $WAV_FILE" > test_data/wav.scp
EOF
cat << "EOF" >> test.bash
steps/online/nnet3/decode.sh --cmd "run.pl" --nj 1      \
     --acwt 1.0 --post-decode-acwt 10.0                 \
     exp/tdnn_7b_chain_online/graph_pp                  \
     test_data                                          \
     exp/tdnn_7b_chain_online/decode_out

echo
echo "The decoded speech is:"
cat exp/tdnn_7b_chain_online/decode_out/log/decode.1.log | \
    grep utterId | head -1 | cut -d ' ' -f 2- | pr -to6
EOF
chmod a+x test.bash

#
# Clean up
#
echo "Now, ready to decode the test audio"
echo "  cd s5"
echo "  ./test.bash"
cd - > /dev/null
