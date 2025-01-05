#!/bin/bash
#
# This script is ran in debain
#
set -e

#
# 1.) Start tmux and python3
#
cd $(dirname $0)
pidFile=$(pwd)/vggTmux.pid
./stop-vgg-speaker-recognition-tmux.bash $pidFile

tmux new -s spkrID -d
cmd="cd ~/Projects/vggvox-speaker-identification/"
tmux send-keys -t spkrID "$cmd; tmux wait-for -S done" ENTER\; wait-for done
cmd="source  ~/.virtualenvs/venv3/bin/activate"
tmux send-keys -t spkrID "$cmd; tmux wait-for -S done" ENTER\; wait-for done
tmux send-keys -t spkrID "python" Enter;
sleep 1


#
# 2.) Start vgg "server"
#
cmd="pidFile=\"$pidFile\""
tmux send-keys -t spkrID "$cmd" Enter;
cmd='exec(open("vgg_sre_session_v2.py").read(), globals())'
tmux send-keys -t spkrID "$cmd" Enter;
echo "Starting VGG SPEAKER RECOGNITION TMUX..."

