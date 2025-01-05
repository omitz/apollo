#!/bin/bash
set -ue

LIB_PATH="speaker-id-library/src/main/java/com/caci/apollo/speaker_id_library"
SOURCE_FILES="
        $LIB_PATH/PlaybackListener.java
        $LIB_PATH/SpeakerRecognitionModel.java
        $LIB_PATH/PlaybackThread.java
        $LIB_PATH/RecordingThread.java        
"

CLASS_PATH="speaker-id-library/src/main/java"
CLASS_PATH+=":`find ~/.gradle | grep json-simple-1.1.1.jar | head -1`"
CLASS_PATH+=":`find ~/.gradle | grep gson-2.8.5.jar | head -1`"
CLASS_PATH+=":`find ~/.gradle | grep commons-io-2.6.jar | head -1`"
CLASS_PATH+=":`find ~/.gradle | grep pickle-1.1.jar | head -1`"
CLASS_PATH+=":`find | grep jetified-vosk | grep classes.jar | head -1`"


# create SpeakerIdTest.html 
rm -rf javadoc/ || true
mkdir javadoc/
pygmentize -f html -O style=colorful,linenos=1 \
           -O full -o javadoc/SpeakerIdTest.html \
           speaker-id-library/src/androidTest/java/com/caci/apollo/speaker_id_library/SpeakerIdTest.java

# generate javadoc
ANDROID_JAR=`find $HOME/ | grep -m 1 Sdk/platforms/android-.*/android.jar`
if (! javadoc  -public -notree -nonavbar -noindex -nodeprecated  -d javadoc/ \
      -classpath "$CLASS_PATH" -source 8 \
      -bootclasspath $ANDROID_JAR \
      -Xdoclint:none -overview $LIB_PATH/overview.html $SOURCE_FILES); then
    echo "Please first run: "
    echo "  ./build_and_symlink_aar.bash"
    exit -1
fi

rm speaker-id-library-javadoc.jar || true
cd javadoc/
zip -r ../apollo-speakerId-library-javadoc.jar ./
cd -

google-chrome --new-window javadoc/index.html



