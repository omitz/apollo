#!/bin/bash
#
# This program takes an audio file (not necessary have to be a
# wav format) and convert it to "standard" .wav format used by Kaldi.
#
#
# TC 2020-03-19 (Thu) -- Handle spaces in file name.  Also skip
# converting wav that is already in correct format.  Use ffmpeg
# instead of gstramer. 
#
# Tommy Chang
# 2019-11-06 (Wed)

set -ue


#------------------------------
# 1.) check input parameters
#------------------------------
# specify usage:

if [[ ! $# -eq 2 ]]; then
    cat <<EOF 
Usage:
  $0 <in_audio_file> <out_audio_file>

Example:
  $0 /tmp/test.mp3 /dev/shm/test.wav
EOF
    exit 1;
fi
AUDIO_IN_FILE="$1"
AUDIO_OUT_FILE="$2"


#------------------------------
# 1.) convert Audio to WAV in ram disk
#------------------------------
# 16000 Hz, 16-bit data (good for kaldi)
AUDIO_BASE_NAME=$(basename "$AUDIO_IN_FILE")

if (file "$AUDIO_IN_FILE" | grep \
    "WAVE audio, Microsoft PCM, 16 bit, mono 16000 Hz" > /dev/null); then
    ln -s "$AUDIO_IN_FILE" "$AUDIO_OUT_FILE"
else
    ffmpeg -y -i "$AUDIO_IN_FILE" -ar 16000 -sample_fmt s16 -ac 1 \
           "$AUDIO_OUT_FILE"  2> /dev/null > /dev/null
fi

