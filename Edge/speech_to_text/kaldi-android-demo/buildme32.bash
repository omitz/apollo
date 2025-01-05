#
# Reset
#
#git checkout app/build.gradle  build.gradle models/build.gradle
cp build_original.gradle build.gradle
cp app/build_original.gradle app/build.gradle
cp models/build_original.gradle models/build.gradle

rm -rf .gradle/ gradle/ gradlew

#-------------------------------
# 1.) Update build.gradle
#-------------------------------
sed -i 's|3\.0\.0|3\.5\.1|g' build.gradle


## app/ set SDK to 28
sed -i 's|compileSdkVersion 26|compileSdkVersion 28|g' app/build.gradle
sed -i 's|targetSdkVersion 26|targetSdkVersion 28|g' app/build.gradle
sed -i 's|appcompat-v7:26.1.0|appcompat-v7:28.0.0|g' app/build.gradle

## model/ set SDK to 28
sed -i 's|compileSdkVersion 26|compileSdkVersion 28|g' models/build.gradle
sed -i 's|targetSdkVersion 26|targetSdkVersion 28|g' models/build.gradle

## add platform to 32-bit:
sed -i "\|versionName|a \ 	ndk.abiFilters 'armeabi-v7a'" app/build.gradle
