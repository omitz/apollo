#!/bin/bash
set -e                          # enable error checking

/etc/init.d/clamav-freshclam start # periodically get latest virus database
/etc/init.d/clamav-freshclam status # catch error

/etc/init.d/clamav-daemon start
/etc/init.d/clamav-daemon status # catch error

python main.py --useS3 True
