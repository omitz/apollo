#!/bin/bash

set -ue

cd ../datafeed-api-lib/
./build_and_symlink_latest_missionapi_aar.bash
./build_and_symlink_aar.bash
cd -
