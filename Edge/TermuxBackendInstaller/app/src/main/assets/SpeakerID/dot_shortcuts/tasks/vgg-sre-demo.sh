#!/data/data/com.termux/files/usr/bin/bash
#
# Put this in Termxux home folder ~/.shortcuts/tasks/
#
# if [ $# -eq 1 ]; then
#     # call from within webview app
#     WEBOPT=$1
# else
#     # call from Termux:Widget launcher
#     WEBOPT=""
# fi

#
# 1.) Start the vgg speaker recognition front-end
#
#termux-toast "start"

cd /data/data/com.termux/files/home/projects/speakerID
PID=$(ps x | grep speaker_recognition_gui | grep python | awk '{print$1;}')
# if [ -n "$PID" ]; then
#     killall -n $PID -w python3  # wait for exsting speaker ID to be killed
#     tmux wait-for -U my3; tmux wait-for -U my3 # clean up in case tmux locks
# fi

while [ -n "$PID" ]; do
     kill $PID
     echo "waiting for speaker id to die"
     usleep 100000
     PID=$(ps x | grep speaker_recognition_gui | grep python | awk '{print$1;}')
done


./speaker_recognition_gui.py $@ &

