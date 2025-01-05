#!/bin/bash

VIP="Aishwarya_Rai_Bachchan
Frankie_Muniz
Jim_Gaffigan
Leonardo_DiCaprio
Liza_Minnelli
Ewan_McGregor
Haley_Joel_Osment
Katie_Holmes
Liam_Neeson
Mohammad_Ali"

echo "This will destroy unknown/ fodler, hit control-c to stop"
read

DATADIR=../../dataSets/lfw
DIRS=`ls -1 $DATADIR/ | shuf | head -100`

for dir in $DIRS; do
    if [[ "$VIP" =~ "$dir" ]] ; then
        echo  "$dir already in VIP, try again"
        exit 1
    fi
done

for dir in $DIRS; do
    file=`ls -1 $DATADIR/$dir | head -1`
    echo $file
    mkdir -p unknown_dataset/$dir
    cp $DATADIR/$dir/$file unknown_dataset/$dir/
done
