#!/bin/bash
#
# This script takes an input directory and outputs data in csv format:
#    filename,speaker
#
# Example:
#   echo "filename,speaker" > cfg_apollo/test_list.csv
#   ./create_test_list.bash vip >> cfg_apollo/test_list.csv
#   ./create_test_list.bash add_vip >> cfg_apollo/test_list.csv
#
# TC 2020-03-11 (Wed)
if ! [ $# -eq 1 ]; then
    cat <<EOF 
Usage:
  $0 <dir> 

Example:
  $0 vip
EOF
    exit 0;
fi

DIR=$1

# 1.) Get a list of wav files:
files=$(find $DIR/ -size +128200c | grep wav$)

# 2.) Get a list of names
names=$(echo "$files" | cut -d / -f 2)

# 3.) Concatenate files and names with comma
paste -d ',' <(echo "$files") <(echo "$names")
