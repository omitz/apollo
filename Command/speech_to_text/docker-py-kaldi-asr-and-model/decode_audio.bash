#!/bin/bash
#
# This program takes a speech audio file (not necessary have to be a
# wav format) and returns its transcription.
#
#
# TC 2020-03-19 (Thu) -- Handle spaces in file name.  Also skip
# converting wav that is already in correct format.  Use ffmpeg
# instead of gstramer.  Run example:
#
#  /usr/bin/time --verbose docker run -it --rm -v /share/Downloads:/data --shm-size=512m apollo/py-kaldi-asr ./decode_audio.bash "/data/2020-03-18 Apollo CDR.mp4" > out.txt
#
#
# Tommy Chang
# 2019-11-06 (Wed)

set -e

function clean_up ()
{
    local  output=$1
    local  server_pid=$2
    local  audio_file_converted="$3"

    rm $output
    kill $server_pid
    if [[ "$alreadyWav" == "false" ]]; then
        rm "$audio_file_converted"
    fi
}


#------------------------------
# 1.) check input parameters
#------------------------------
# specify usage:

if [[ ! $# -eq 1 ]]; then
    cat <<EOF 
Usage:
  $0 <audio_file> 

Example:
  $0 /tmp/test.wav
EOF
    exit 1;
fi
EXEC_DIR=$(pwd)
PROG_DIR=$(dirname $(readlink -f "$0"))
AUDIO_FILE="$1"


#------------------------------
# 1.5.) convert Audio to WAV in ram disk
#------------------------------
# 16000 Hz, 16-bit data (good for kaldi)
if [[ "$AUDIO_FILE" =~ "/dev/shm/" ]]; then
    AUDIO_FILE_CONVERTED="${AUDIO_FILE}_$$.wav"
else
    AUDIO_BASE_NAME=$(basename "$AUDIO_FILE")
    AUDIO_FILE_CONVERTED="/dev/shm/${AUDIO_BASE_NAME}_$$.wav"
fi

if (file "$AUDIO_FILE" | grep \
    "WAVE audio, Microsoft PCM, 16 bit, mono 16000 Hz" > /dev/null); then
    alreadyWav=true
    AUDIO_FILE_CONVERTED="$AUDIO_FILE"
else
    ffmpeg -i "$AUDIO_FILE" -ar 16000 -sample_fmt s16 -ac 1 \
           "$AUDIO_FILE_CONVERTED"  2> /dev/null > /dev/null
    alreadyWav=false
fi

#------------------------------
# 2.) start the server in background
#------------------------------
pkill asr_server || true
python ./asr_server.py 2> /dev/null &
server_pid=$!
while (! lsof -i :80 > /dev/null); do
    sleep 1
done



#------------------------------
# 3.) run the client in foreground
#------------------------------
output=/dev/shm/out.$$
if (! ./asr_client.py -p 80 "$AUDIO_FILE_CONVERTED" 2> $output); then
    cat $output
    clean_up $output $server_pid "$AUDIO_FILE_CONVERTED"
    exit 1
fi

# The decoded text start with the prompt "INFO:root:** hstr          :"
grep 'hstr          :' $output | sed 's/INFO:root:\*\* hstr          ://g'


#------------------------------
# 4.) clean up
#------------------------------
clean_up $output $server_pid "$AUDIO_FILE_CONVERTED"
