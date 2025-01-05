#!/bin/bash
#
# Run this script from Temux home directory.  ie
#   projects/speakerID/backup_speaker_recognizer_backend.bash
#
set -ue

export TS=`date +"%Y-%m-%dT%H-%M-%S"`

rm -rf projects/AnLinux/debian/debian-fs/root/Projects/vggvox-speaker-identification/__pycache__/
rm -rf projects/speakerID/__pycache__
echo $TS > projects/speakerID/VERSION.txt

tar -czvf speaker_recognizer_backend_${TS}.tgz \
	projects/speakerID/backup_speaker_recognizer_backend.bash \
	.termux/ .shortcuts/ \
	projects/speakerID projects/AnLinux/debian/debian-fs/root/Projects/vggvox-speaker-identification/ 
