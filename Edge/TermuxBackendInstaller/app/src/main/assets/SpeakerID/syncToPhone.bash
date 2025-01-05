#!/bin/bash
rm speaker_recognizer_backend_*.tgz
projects/speakerID/backup_speaker_recognizer_backend.bash
cat speaker_recognizer_backend_*.tgz | ssh -p8022 user@localhost 'tar xvzf -'
