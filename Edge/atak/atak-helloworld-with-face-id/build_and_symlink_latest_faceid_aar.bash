#!/bin/bash
set -e

# 1. build the aar at FaceIdBackend
cd ../../../Edge/libs/FaceIdBackend/
./build_and_symlink_aar.bash
cd -

# 2. clean up
echo "done"
