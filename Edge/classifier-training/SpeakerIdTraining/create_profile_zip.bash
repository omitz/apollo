#!/bin/bash -e
set -e

if [[ ! $# -eq 1 ]]; then
    cat <<EOF 
Usage:
  $0 <vip_with_profile_dir>

Example:
  $0 vips_with_profile/
EOF
    exit 1;
fi
EXEC_DIR=$(pwd)
PROG_DIR=$(dirname $(readlink -f "$0"))
PROFILE_DIR=$(readlink -f "$1")


# make a temporary directory:
TMP_DIR=tmp_$$
mkdir $TMP_DIR

# create a zip file
cd $TMP_DIR
ln -s $PROFILE_DIR profiles
zip profiles.zip profiles/*/profile.jpg
cd -

# save the zip file
mv $TMP_DIR/profiles.zip .

# cleanup
rm -rf $TMP_DIR/
echo "created profiles.zip"


