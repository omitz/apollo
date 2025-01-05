#!/bin/bash
#
# Create a data/mission package consists of SpeakerID classifier
# models files.
# 
# TC 2021-06-02 (Wed)

set -eu

if ! [ $# -eq 3 ]; then
    cat <<EOF 
Usage:
  $0 <model_dir> <data_package_uuid> <data_pacakge_zip>

Example:
  $0 clasifier_models/celebrity10/ speakerID_celebrity10 atak_speakerID_celebrity10.zip
EOF
    exit -1;
fi
ORIDIR=$(pwd -P)
EXEDIR=$(dirname $0)
MODELDIR=$(readlink -f $1)
UUID=$2
ZIPFILE=$3

# 1.) Create a directory for data package
tmpDataPackageDir=data_package_dir_$$
rm -rf $tmpDataPackageDir/ || true
mkdir -p $tmpDataPackageDir/MANIFEST/

# 2.) copy the model files 
cp $MODELDIR/svm_vosk.json $tmpDataPackageDir/svm_vosk
cp $MODELDIR/meta_vosk.pkl $tmpDataPackageDir/meta_vosk
cp $MODELDIR/profiles.zip $tmpDataPackageDir/profiles

# 3.) create some kind of info file
TIME_STAMP=$(date +%s.%3N)
cat << EOF > $tmpDataPackageDir/info
{
    "title": "SpeakerID with 10 VIP"
    "version": "1"
    "timeStamp": "$TIME_STAMP"
}
EOF

# 4.) create the manifest file
cat << EOF > $tmpDataPackageDir/MANIFEST/manifest.xml
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

# 5.) zip the data pacakge
cd $tmpDataPackageDir/
#latest=$(find -L $MODELDIR -maxdepth 5 -printf "%C@\t%p\n" | sort -n | tail -1 | cut -f 2)
zip -r $MODELDIR/${ZIPFILE} *
#test -f $MODELDIR/${ZIPFILE}
#touch -r $latest $MODELDIR/${ZIPFILE}
cd -

# 5.) clean up
rm -rf $tmpDataPackageDir/
echo "Created ${UUID}.zip"
