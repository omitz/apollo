#!/bin/bash

if ! [ $# -eq 1 ]; then
    cat <<EOF 
Usage:
  $0 <output_tar_ball_name> 

Example:
  $0 atak_helloworld_with_speakerId_0p9p0.tgz
EOF
    exit 0;
fi

cd helloworld/
rm -rf .gradle/ .idea/ local.properties build/ app/build/ \
   ./app/app.iml *.iml ./app/libs app/.cxx/ ; find | grep build$ | \
    xargs rm -rf; find | grep iml$ |xargs  rm; find ./ -type d | \
    grep .cxx$ | xargs rm -rf; find ./ | grep hprof$ | xargs rm -rf
cd -

tar -hcvzf $1 --exclude '*release.aar' --exclude 'sdk_4p3*' helloworld/ \
    build_apk.bash README.md \
    --transform 'flags=r;s,^,apollo_dataFeed/,'
