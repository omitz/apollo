

# Automatic run and test
```
./build_and_run_test.bash
./build_and_run_app.bash
./build_celebrity10_model_installer.bash
```

`./build_and_run_test.bash` runs the instrumented test, which run on
the phone/emulator.

`./build_and_run_app.bash` runs an GUI app and uses the celebrity10 model that
comes with the libary (in the library's Assets folder).

`./build_celebrity10_model_installer.bash` creates the celebrity10
model installer apk.  You can use this apk to install the model
instead of using ATAK to import data package.



# How to build app
```
/gradlew --no-daemon clean depend
./gradlew --no-daemon :app:assembleDebug
adb uninstall com.caci.apollo.faceidbackend
adb install ./app/build/outputs/apk/debug/app-debug.apk
adb shell am start --user 0 -n com.caci.apollo.faceidbackend/.MainActivity

```

# How to build libary
```
./gradlew --no-daemon :face-id-library:assembleDebug
./gradlew --no-daemon :face-id-library:assembleRelease
```

Output is at `face-id-library/build/outputs/aar/face-id-library-debug.aar`
Output is at `face-id-library/build/outputs/aar/face-id-library-release.aar`

# How to run junit test
```
./gradlew face-id-library:testDebug --tests=FaceIdTest.test1
```

# How to run instrumented test 

Make sure to change build.gradle not to use embed dependencies.
```
./gradlew face-id-library:connectedDebugAndroidTest 

```

Or
```
./gradlew face-id-library:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.caci.apollo.face_id_library.FaceIdTest#test1

```
OR
```
./gradlew face-id-library:assembleAndroidTest
./gradlew face-id-library:installDebugAndroidTest

adb shell pm list instrumentation
adb shell am instrument -w \
  -e class com.caci.apollo.face_id_library.FaceIdTest#test1 \
  com.caci.apollo.face_id_library.test/androidx.test.runner.AndroidJUnitRunner
  
```

# How to update latest aar from commandline
edit app/build.gradle:

```
//    implementation project (':face-id-library')
    implementation project (':aars:face-id-library-debug')
```


```
./gradlew --no-daemon clean depend
./gradlew --no-daemon :face-id-library:assembleDebug
cp face-id-library/build/outputs/aar/face-id-library-debug.aar aars/face-id-library-debug/
./gradlew --no-daemon :app:assembleDebug

adb uninstall com.caci.apollo.faceidbackend
adb install ./app/build/outputs/apk/debug/app-debug.apk
adb shell am start --user 0 -n com.caci.apollo.faceidbackend/.MainActivity

```
# How to add/update model to assets

Edit file `face-id-library/src/main/assets/sync/assets.lst`


The resulting file asset is located at:
```
/storage/emulated/0/Android/data/com.caci.apollo.face_id_library.test/files/sync/
/storage/emulated/0/Android/data/com.caci.apollo.faceidbackend/files/sync/
```

# S3 bucket locations

     S3 Buckets:
     - https://s3.console.aws.amazon.com/s3/buckets/termuxpackagedir?prefix=atak/faceID_v2/&showversions=false
     - https://s3.console.aws.amazon.com/s3/buckets/termuxpackagedir?prefix=atak/faceID_aar/&showversions=false

