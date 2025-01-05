#!/bin/bash
#
# Create a data/mission package consists of SpeakerID classifier
# models files.
# 
# TC 2021-06-02 (Wed)

set -eu

# 0.) Check commandline arguments
if ! [ $# -eq 1 ]; then
    cat <<EOF 
Usage:
  $0 <data_package_uuid> 

Example:
  $0 speakerID_celebrity10
EOF
    exit 0;
fi
ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)
UUID=$1



# 1.) Create a directory for data package
rm -rf data_package/ || true
mkdir -p data_package/MANIFEST/

# 2.) copy the model files 
cp svm_vosk.json data_package/svm_vosk
cp meta_vosk.pkl data_package/meta_vosk
cp profiles.zip data_package/profiles

# 3.) create some kind of info file
TIME_STAMP=$(date +%s.%3N)
cat << EOF > data_package/info
{
    "title": "SpeakerID with 10 VIP"
    "version": "1"
    "timeStamp": "$TIME_STAMP"
}
EOF

# 3.) create the manifest file
cat << EOF > data_package/MANIFEST/manifest.xml
<MissionPackageManifest version="2">
   <Configuration>
      <Parameter name="uid" value="${UUID}"/>
      <Parameter name="name" value="ApolloSpeakerID-model"/>
      <Parameter name="onReceiveDelete" value="false"/>
   </Configuration>
   <Contents>
      <Content ignore="false" zipEntry="info"/>
      <Content ignore="false" zipEntry="svm_vosk"/>
      <Content ignore="false" zipEntry="meta_vosk"/>
      <Content ignore="false" zipEntry="profiles"/>
   </Contents>
</MissionPackageManifest>
EOF

# 4.) zip the data pacakge
cd data_package/
# zip -r ../apolloSpeakerId-data-package.zip *
zip -r ../atak_uuid=${UUID}.zip *
cd -

# 5.) clean up
# echo "Created apolloSpeakerId-data-package.zip"
echo "Created ${UUID}.zip"
