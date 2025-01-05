#!/bin/bash
#
# Here we train the corpus.  We assume that prepare.bash has already
# been run.
#
# TC 2019-10-10 (Thu) 
set -e

#
# 1.) Set up the test data
#
# We must create these 3 files:
# wav.scp:
#    <utt_id> <path_to_wav_file>
#    NOTE: If <path_to_wav_file> is a shp file, then, it can be a command:
#          "sph2pipe -f wav -p -c 1 path_to_shp_file |"
#    example:
#    "fash-an251-b ~/bin/sph2pipe -f wav -p -c 1 ~/data/an251-fash-b.sph |"
# utt2spk:
#    <utt_id> <spkr>
#    example:
#    "fash-an251-b fash"
# spk2utt:
#    <spkr> <utt_id1> <utt_id2> <utt_id3>
#    example:
#    "fash fash-an251-b fash-an253-b fash-an254-b fash-an255-b"
#
mkdir -p data/test
wavFile=$(head -1 ../an4/s5/data/test/wav.scp | cut -d ' ' -f 2- )
echo "utterId $wavFile" > data/test/wav.scp
echo "spkId utterId" > data/test/spk2utt
echo "utterId spkId" > data/test/utt2spk


# 2.) Compute MFCC features.  
##
# Prepare a directory to save the features
featdir=/tmp/delme_test/
rm -rf $featdir
mkdir $featdir
##
# creating MFCC features:
cp ../an4/s5/conf/mfcc.conf ./conf/
steps/make_mfcc.sh --nj 1 --cmd "run.pl"	\
		   data/test			\
		   exp/make_mfcc/test		\
		   $featdir; 

#
# 3.)
# creating CMVN stats:
steps/compute_cmvn_stats.sh data/test exp/make_mfcc/test $featdir; 


#
# 4.) decode the test data: 
# We need the exp/ directory structure.
#
rm -rf exp/mono/decode
cp ../an4/s5/conf/decode.config ./conf/
mkdir -p ./local; echo '#!/bin/bash' > local/score.sh; chmod a+x local/score.sh
steps/decode.sh --config conf/decode.config --nj 1 --cmd "run.pl"	\
		exp/mono/graph						\
		data/test						\
		exp/mono/decode 

## Show the result:
cat exp/mono/decode/log/decode.1.log | grep utterId | head -1

##??? lattice-best-path ark:'gunzip -c ./exp/mono/decode/lat.1.gz |' 'ark,t: | int2sym.pl -f 2- ./mono_graph/words.txt'
