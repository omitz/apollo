#!/bin/bash
#
# This program adds more new data for each vip
#
#TC 2021-09-09 (Thu) --

set -ue

N_DATA=20
LARGE_VIP_DIR="/share/Projects/apollo/APOLLO.master/Command/vip/vips_large"
function getNewData () {
    local -n A=$1

    A=`ls -1 $LARGE_VIP_DIR/$vip/ | shuf | head -$N_DATA`
    for a in $A; do
        filename=$(basename $a)
        if (test -e vips_with_profile/$vip/$filename); then
            echo "already exists $filename"
            return -1
        fi
    done
    return 0
}


for vipFull in vips_with_profile/*; do
    vip=$(basename $vipFull)
    echo "vip = $vip"
    newData=""
    while ! getNewData newData; do
        echo "try again for all new data, $vip"
    done
    for file in $newData; do
        cp $LARGE_VIP_DIR/$vip/$file vips_with_profile/$vip/
    done
done
