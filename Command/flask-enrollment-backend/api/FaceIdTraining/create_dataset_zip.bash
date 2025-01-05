#!/bin/bash
#
# Create a enrollment dataset.  The resulting zip file has the same
# timestamp as the directory.
# 
# TC 2021-06-14 (Mon) 
set -ue

# 0.) Check commandline arguments
if ! [ $# -eq 2 ]; then
    cat <<EOF 
Usage:
  $0 <enrollment_dir> <out_zip_file> 

Example:
  $0 enrollments/celebrity10/  celebrity10_dataset.zip
EOF
    exit -1
fi
ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)
ENROLLDIR=$1
ZIPFILE=$(readlink -f $2)

dName=$(dirname $ENROLLDIR)
bName=$(basename $ENROLLDIR)

cd $dName
latest=$(find -L $bName -maxdepth 5 -printf "%C@\t%p\n" | sort -n | tail -1 | cut -f 2)
zip -r $ZIPFILE $bName
test -f $ZIPFILE
touch -r $latest $ZIPFILE
cd -

echo "created $ZIPFILE"
