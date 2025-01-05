#!/bin/bash
set -ue

VIP="id00149
id02685
id02881
id03299
id04086
id04653
id05124
id05160
id05227
id06212"

echo "This will destroy unknown/ fodler, hit control-c to stop"
read


DATA_DIR=../../dataSets/vggvox2/aac
DIRS=`ls -1 $DATA_DIR | shuf | head -100`
for dir in $DIRS; do
    id=$dir
    if [[ "$VIP" =~ $dir ]]; then
        echo "$dir already in VIP, Please try again"
        exit 1
    fi
done

for dir in $DIRS; do
    id=$dir
    youtube=`ls -1 $DATA_DIR/$dir | shuf | head -1`

    ## make sure audio > 4 sec
    duration=0
    while (( $(echo "$duration < 4" | bc -l) )); do
        file=`ls -1 $DATA_DIR/$dir/$youtube | shuf | head -1`
        duration=`./midentify.bash $DATA_DIR/$dir/$youtube/$file | grep LENGTH | cut -d = -f 2`
        echo "duration = $duration"
    done
    echo $id
    echo $file
    mkdir -p unknown_dataset/$dir/
    cp $DATA_DIR/$dir/$youtube/$file unknown_dataset/$dir/
done
