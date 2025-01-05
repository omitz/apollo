#!/bin/bash
set -e

# 1. build the aar at Mission API
cd ../mission-api-lib/
./build_and_symlink_aar.bash
cd -

# 2. clean up
echo "done"
