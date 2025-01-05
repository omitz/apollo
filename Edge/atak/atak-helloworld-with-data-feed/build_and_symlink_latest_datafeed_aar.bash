#!/bin/bash
set -e

# 1. build the aar at datafeed (including missionapi)
cd ../datafeed-api-lib/
./build_and_symlink_latest_missionapi_aar.bash
./build_and_symlink_aar.bash
cd -

# 2. clean up
echo "done"
