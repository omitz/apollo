#!/bin/bash
set -e

LIB_PATH="data-feed-library/src/main/java/com/caci/apollo/datafeed/"
SOURCE_FILES="
        $LIB_PATH/DataFeedAPI.java
        $LIB_PATH/DataFeedUtil.java
"

CLASS_PATH="data-feed-library/src/main/java"
CLASS_PATH+=":`find ~/.gradle | grep json-simple-1.1.1.jar | head -1`"
CLASS_PATH+=":`find | grep mission-api-library/jars/classes.jar | head -1`"
CLASS_PATH+=":sdk/main.jar"

# create SpeakerIdTest.html 
rm -rf javadoc/ || true
mkdir javadoc/
pygmentize -f html -O style=colorful,linenos=1 -O full -o javadoc/README.html README.md

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

rm *-javadoc.jar || true
cd javadoc/
zip -r ../apollo-datafeed-library-javadoc.jar ./
cd -

google-chrome --new-window javadoc/index.html
