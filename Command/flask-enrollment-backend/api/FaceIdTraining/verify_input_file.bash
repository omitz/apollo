#!/bin/bash

set -e

if [[ ! $# -eq 2 ]]; then
    cat <<EOF 
Usage:
  $0 <input_image> <json_out_file>

Example:
  $0 test.jpg out.json
EOF
    exit 1;
fi
PROG_DIR=$(dirname $(readlink -f "$0")) # where is the program located
EXEC_DIR=$(pwd -P)                      # where are we executing from

inputFile=$1
JSON_OUT=$2

echo | $PROG_DIR/verify_input_file.py $inputFile $JSON_OUT

