#!/bin/bash
#
#
# 
#
set -ue

# creates the product.inf file given a list of apks. Optionally creates the INFZ (zipped OTA repo)

AAPT=`which aapt`
#AAPT=~/Android/Sdk/build-tools/28.0.3/aapt 

extract-apk-icon () {
    unzip -p $1 $($AAPT d --values badging $1 |
                      sed -n "/^application: /s/.*icon='\([^']*\).*/\1/p") > $2;
}


if [[  "$1" == "" ]];
then 
    echo "./generate-inf.sh [staging directory containing APK files] [true | false:  create INFZ]"
    exit 1
fi

if [[ ! -f $AAPT ]];
then 
    echo "cannot find $AAPT"
    exit 1
fi


rm $1/product.inf | true
rm $1/product.infz | true
echo "#platform (Android Windows or iOS), type (app or plugin), full package name, display/label, version, revision code (integer), relative path to APK file, relative path to icon file, description, apk hash, os requirement, tak prereq (e.g. plugin-api), apk size" >> $1/product.inf

for FILE in $1/*;do

   echo "processing: $FILE"

   if [[ $FILE == *apk ]];
   then

       PACKAGE_LINE=`$AAPT dump badging $FILE | grep ^package`
       IFS=';' tmparray=($(echo "$PACKAGE_LINE" | tr "'" ";"))
       
       PACKAGE_NAME=${tmparray[1]}
       PACKAGE_VERSIONCODE=${tmparray[3]}
       PACKAGE_VERSIONNAME=${tmparray[5]}

       ANDROID_VERSION_LINE=`$AAPT dump badging $FILE | grep sdkVersion`
       NAME_LINE=`$AAPT dump badging $FILE | grep "application-label:"`
       IFS=';' tmparray=($(echo "$ANDROID_VERSION_LINE $NAME_LINE" | tr "'" ";"))
       ANDROID_VERSION=${tmparray[1]}
       NAME=${tmparray[3]}
       
       
       TMP=`$AAPT dump --include-meta-data badging $FILE | grep app_desc`
       IFS=';' descarr=($(echo "$TMP" | tr "'" ";"))
       DESC=${descarr[3]//,/\.}

       GRAPHIC=${FILE/\.apk/\.png/}
       APP=app
       if [[ $FILE == *"Plugin"* ]]; then
        APP=plugin
       fi

       if [[  "$DESC" == "" ]];
       then 
          DESC="No description supplied for $NAME $PACKAGE_VERSIONNAME"
       fi

       SHA256=`shasum -a 256 $FILE | awk '{ print $1 }'`
       FILESIZE=$(wc -c < "$FILE")

#       echo ">> $PACKAGE_NAME $PACKAGE_VERSIONCODE $PACKAGE_VERSIONNAME $NAME $ANDROID_VERSION"


       # determine the plugin api
       papi=`$AAPT dump --include-meta-data badging $FILE | grep plugin-api`
       IFS=';' papiarr=($(echo "$papi" | tr "'" ";"))
       PLUGINAPI=${papiarr[3]//,/\.}
       # echo ">> $PLUGINAPI"
        

       entry="Android,$APP,$PACKAGE_NAME,$NAME,$PACKAGE_VERSIONNAME,$PACKAGE_VERSIONCODE,`basename $FILE`,`basename $GRAPHIC`,$DESC,$SHA256,$ANDROID_VERSION,$PLUGINAPI,$FILESIZE"
       echo $entry >> $1/product.inf
       echo "generating entry: $entry"


#      if [[  "$2" == "true" ]];
#      then 
          #also dump app icon
          filename=$(basename "$FILE")
          filename="${filename%.*}.png"

          echo "extracting $filename"
          extract-apk-icon $FILE $1/$filename
#      fi
   fi
done

if [[  "$2" == "true" ]];
then 
   echo "generating infz"
   zip -r -j --exclude=*.apk* --exclude=*.DS_Store* $1/product.infz  $1
fi

echo "Note, currently you may need to populate the app description (column I) if the app developer did not add this information into their AndroidManifest.xml file."
