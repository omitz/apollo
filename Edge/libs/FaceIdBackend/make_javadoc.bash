#!/bin/bash
set -ue

LIB_PATH="face-id-library/src/main/java/com/caci/apollo/face_id_library"
SOURCE_FILES="
        $LIB_PATH/FaceRecognitionModel.java
"

CLASS_PATH="face-id-library/src/main/java"
CLASS_PATH+=":`find | grep tensorflow-android | grep -m 1 classes.jar`"


# create FaceIdTest.html
rm -rf javadoc/ || true
mkdir javadoc/
pygmentize -f html -O style=colorful,linenos=1 \
           -O full -o javadoc/FaceIdTest.html \
           face-id-library/src/androidTest/java/com/caci/apollo/face_id_library/FaceIdTest.java

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

rm apollo-faceId-library-javadoc.jar || true
cd javadoc/
zip -r ../apollo-faceId-library-javadoc.jar ./
cd -

google-chrome --new-window javadoc/index.html
