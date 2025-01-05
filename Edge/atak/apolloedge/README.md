# Apollo Edge ATAK Plugin Build Instruction

## Emulator Setup

For the Debian container to work properly in the emulator, make
sure you disable selinux.  You can do this by adding the `"-selinux permissive"` command line option.  For example:
```
cd /Users/Wole/Library/Android/sdk/emulator/bin/
./emulator -writable-system -selinux permissive  -avd WolePixel3_API_27_x86_64
```


## One-step build
```
./build_apk.bash
```

## Multi-step buld:
### Build Depedend Libraries
We need build the speakerID, faceID, datafeed AARs.

```
cd aars/apollo-faceId-library
./rebuildSymlink.bash
cd -
```

```
./build_and_symlink_latest_speakerid_aar.bash
./build_and_symlink_latest_datafeed_aar.bash
```


### Build the Plugin 

Make sure you have already built speakerId and faceId in the
previous steps.

```
./gradlew --no-daemon clean depend
./gradlew --no-daemon packCivDebug
```
  
The apk file will generated to something like below:
```
./app/build/outputs/apk/civ/debug/apollo-edge-0.9.5-civ-debug.apk
```
   

## Installation Instruction

First install ATAK-Civ 4.3.0.
```
adb install atak_4p3_04122021.apk
```

### Remove old packages, if needed
```
    adb uninstall com.atakmap.android.apolloedge.plugin
```


### Install Apollo Edge plugin:

To install the plugin, do something simliar to below:
```
adb install app/build/outputs/apk/civ/debug/apollo-edge-0.9.5-civ-debug.apk
```

