#!/data/data/com.termux/files/usr/bin/bash

PID=$(ps x | grep speaker_recognition_gui | grep python | awk '{print$1;}')
if [ -n "$PID" ]; then
    echo $PID
#    kill -s SIGUSR1 $PID
    kill $PID
fi

