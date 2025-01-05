

# Automatic build, run, and test
```
./build_and_run_test.bash
./build_and_run_app.bash
./build_celebrity10_model_installer.bash
```

`./build_and_run_test.bash` runs the instrumented test, which
runs on the phone/emulator.

`./build_and_run_app.bash` runs an GUI app and uses the celebrity10 model that
comes with the libary (in the library's Assets folder).

`./build_celebrity10_model_installer.bash` creates the celebrity10
model installer apk.  You can use this apk to install the model
instead of using ATAK to import data package.




# Reference
## How to build app
```
./gradlew --no-daemon clean depend
./gradlew --no-daemon :app:assembleDebug
adb uninstall com.caci.apollo.speakeridbackend
adb install ./app/build/outputs/apk/debug/app-debug.apk
adb shell am start --user 0 -n com.caci.apollo.speakeridbackend/.MainActivity

```

## How to build libary
```
./gradlew --no-daemon :speaker-id-library:assembleDebug
./gradlew --no-daemon :speaker-id-library:assembleRelease
```

Output is at `speaker-id-library/build/outputs/aar/speaker-id-library-debug.aar`
Output is at `speaker-id-library/build/outputs/aar/speaker-id-library-release.aar`

## How to run junit test
```
./gradlew speaker-id-library:testDebug --tests=SpeakerIdTest.test1
```

## How to run instrumented test 

Make sure to change build.gradle not to use embed dependencies.
```
./gradlew speaker-id-library:connectedDebugAndroidTest 

```

Or
```
./gradlew speaker-id-library:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.caci.apollo.speaker_id_library.SpeakerIdTest#test1

```
OR
```
./gradlew speaker-id-library:assembleAndroidTest
./gradlew speaker-id-library:installDebugAndroidTest

adb shell pm list instrumentation
adb shell am instrument -w \
  -e class com.caci.apollo.speaker_id_library.SpeakerIdTest#test1 \
  com.caci.apollo.speaker_id_library.test/androidx.test.runner.AndroidJUnitRunner
  
```

## How to update latest aar from commandline
edit app/build.gradle:

```
//    implementation project (':speaker-id-library')
    implementation project (':aars:speaker-id-library-debug')
```


```
./gradlew --no-daemon clean depend
./gradlew --no-daemon :speaker-id-library:assembleDebug
cp speaker-id-library/build/outputs/aar/speaker-id-library-debug.aar aars/speaker-id-library-debug/
./gradlew --no-daemon :app:assembleDebug

adb uninstall com.caci.apollo.speakeridbackend
adb install ./app/build/outputs/apk/debug/app-debug.apk
adb shell am start --user 0 -n com.caci.apollo.speakeridbackend/.MainActivity

```
## How to add/update model to assets

Edit file `speaker-id-library/src/main/assets/sync/assets.lst`


The resulting file asset is located at:
```
/storage/emulated/0/Android/data/com.caci.apollo.speaker_id_library.test/files/sync/
/storage/emulated/0/Android/data/com.caci.apollo.speakeridbackend/files/sync/
```
