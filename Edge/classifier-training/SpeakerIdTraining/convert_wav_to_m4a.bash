#!/bin/bash
#
# This script randomly picks 30 wav files to convert to m4a format and
# delete the rest.
#
# TC 2021-05-18 (Tue)

set -e

if ! [ $# -eq 1 ]; then
    cat <<EOF 
Usage:
  $0 <vip_dir> 

Example:
  $0 vips_with_profile
EOF
    exit 0;
fi
VIP_DIR=$1


DIRs=`find $VIP_DIR/* -type d`
for DIR in $DIRs; do
    echo "DIR = $DIR"
    echo "press enter to continue"
    read
    cd $DIR
    A=`find ./ -type f | grep wav$ | shuf -n 30`
    for f in $A; do
        ffmpeg -i $f ${f//.wav/.m4a};
    done
    rm *.wav || true
    cd -
done
