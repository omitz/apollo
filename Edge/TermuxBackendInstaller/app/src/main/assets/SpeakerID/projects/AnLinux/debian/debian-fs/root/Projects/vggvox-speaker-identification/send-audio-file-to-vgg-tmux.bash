#!/bin/bash
#
# This script is ran in debain
#
set -e

## check parameter
if ! [ $# -eq 3 ]; then
    cat <<EOF 
Usage:
  $0 <sound_File> <out_file> <done_file>

Example:
  $0 ~/Projects/vggvox-speaker-identification/unknown_eval_set/Aishwarya_Rai_Bachchan_00387.wav ~/out.csv ~/done.pid
EOF
    exit 0;
fi
ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)
SOUNDFILE=$1
OUTFILE=$2
DONEFILE=$3


## Make sure the tmux is ready.
pidFile=$EXEDIR/vggTmux.pid
#echo "Checking pid file $pidFile"
count=0
while ! [ -s $pidFile ]; do
    echo "waiting for VGG SPEAKER RECOGNITION TMUX $count"
    count=$((count + 1))
    sleep 3
done
#echo "VGG SPEAKER RECOGNITION TMUX READY!"

## Remove any previous output:
rm -f $OUTFILE 
rm -f $DONEFILE 

## Send the audio file
cmd="identify_speaker (\"$SOUNDFILE\", \"$OUTFILE\", \"$DONEFILE\")"
tmux send-keys -t spkrID "$cmd" Enter;

## Wait for the answer
sleep 1
count=0
while ! [ -s $DONEFILE ]; do
    echo "waiting Answer $count"
    count=$((count + 1))
    sleep 1
done

## show the answer
cat $OUTFILE

