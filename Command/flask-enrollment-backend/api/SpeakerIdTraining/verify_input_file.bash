#!/bin/bash

set -eu

if [[ ! $# -eq 3 ]]; then
    cat <<EOF 
Usage:
  $0 <input_audio> <max_audio_sec> <json_out_file>

Example:
  $0 test.wav 30 out.json
EOF
    exit 1;
fi
PROG_DIR=$(dirname $(readlink -f "$0")) # where is the program located
EXEC_DIR=$(pwd -P)                      # where are we executing from

inputFile=$1
JSON_OUT=$2
MAX_AUDIO_SEC=$3

echo | $PROG_DIR/verify_input_file.py $inputFile $MAX_AUDIO_SEC $JSON_OUT

