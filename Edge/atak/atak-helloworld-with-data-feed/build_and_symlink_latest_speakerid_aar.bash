#!/bin/bash
set -e

# 1. build the aar at SpeakerIdBackend
cd ../../../Edge/libs/SpeakerIdBackend/
./build_and_symlink_aar.bash
cd -

# 2. clean up
echo "done"
