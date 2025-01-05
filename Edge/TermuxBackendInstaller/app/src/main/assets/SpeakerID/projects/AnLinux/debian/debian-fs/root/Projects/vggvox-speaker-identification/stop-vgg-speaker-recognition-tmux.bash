#!/bin/bash
#
# This script is ran in debain
#
set -e

#source  ~/.virtualenvs/venv3/bin/activate
#cd Projects/vggvox-speaker-identification/
#python

#
# 1.) Stop tmux and python3
#
tmux kill-session -t spkrID 2> /dev/null || true
if [ $# -eq 1 ]; then
    pidFile=$1
else
    pidFile=$(pwd)/vggTmux.pid
fi
rm $pidFile 2> /dev/null || true

