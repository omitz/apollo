#!/bin/bash -e
set -ue

if [[ ! $# -eq 2 ]]; then
    cat <<EOF 
Usage:
  $0 <vip_with_profile_dir>  <outfile>

Example:
  $0 enrollments/celebrity10/ profiles.zip

EOF
    exit -1;
fi
EXEC_DIR=$(pwd)
PROG_DIR=$(dirname $(readlink -f "$0"))
PROFILE_DIR=$(readlink -f "$1")
OUTZIP=$2

# make a temporary directory:
TMP_DIR=tmp_$$
mkdir $TMP_DIR

# create a zip file
cd $TMP_DIR
ln -s $PROFILE_DIR profiles
zip profiles.zip profiles/*/profile.jpg
cd -

# save the zip file
mv -f $TMP_DIR/profiles.zip $OUTZIP

# cleanup
rm -rf $TMP_DIR/
echo "created $OUTZIP"


