#!/data/data/com.termux/files/usr/bin/bash
set -eu
MY_NAME=`basename $0`

function err_report {
    echo "Fail @ [${MY_NAME}:${1}]" > $failFilePath
    exit 1
}

trap 'err_report $LINENO' ERR

## To be inserted by the app:
