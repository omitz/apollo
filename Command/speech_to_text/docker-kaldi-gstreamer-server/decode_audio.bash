#!/bin/bash
#
# This program takes a speech audio file (.wav, .mp3, etc) and return
# its transcription.
#
# Tommy Chang
# 2019-11-06 (Wed)

set -e

function clean_up ()
{
    /opt/stop.sh
    rm -f stderr.out master.log worker.log
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
# 2.) start the server in background
#------------------------------
clean_up
/opt/start.sh -y /opt/test/models/nnet2.yaml
while (! cat master.log | grep "New worker" > /dev/null); do
    sleep 1
done

#------------------------------
# 3.) run the client in foreground
#------------------------------
python ./client.py -u ws://localhost/client/ws/speech "$AUDIO_FILE" 2> stderr.out 
ret=$?
if [[ ! $ret -eq 0 ]]; then
    cat stderr.out
    clean_up
    exit 1
fi

#------------------------------
# 4.) clean up
#------------------------------
clean_up
