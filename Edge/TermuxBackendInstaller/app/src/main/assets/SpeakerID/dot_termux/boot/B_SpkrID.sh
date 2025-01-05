#!/data/data/com.termux/files/usr/bin/sh
termux-wake-lock

## Start tmux sessiont as a workaround for runninter termux-api in Android 10
tmux kill-session -t spkrID || true
tmux new -s spkrID -d

## Start back-end "server"
~/projects/speakerID/start-vgg-tensorflow.bash   # this function returns right away

termux-wake-unlock

