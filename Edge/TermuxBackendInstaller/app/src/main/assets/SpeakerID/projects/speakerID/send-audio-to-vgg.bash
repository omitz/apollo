#!/data/data/com.termux/files/usr/bin/bash
#
set -ue

## check parameter
if ! [ $# -eq 3 ]; then
    SOUNDFILE='~/inAudio.wav'
    OUTFILE='~/outSpeaker.csv'
    echo "SOUNDFILE = $SOUNDFILE"
    echo "OUTFILE = $OUTFILE"
    echo "DONEFILE = $DONEFILE"
else
    SOUNDFILE=$1
    OUTFILE=$2
    DONEFILE=$3
fi
ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)


cd ~/projects/AnLinux/debian
./start-debian.sh "~/Projects/vggvox-speaker-identification/send-audio-file-to-vgg-tmux.bash $SOUNDFILE $OUTFILE $DONEFILE" &

