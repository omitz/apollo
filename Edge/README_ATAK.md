(In case we end up needing to integrate with ATAK...)

## Supplemental ATAK plug-in instructions

This README is intended to supplement the ATAK Plugin Development instructions (https://svn.takmaps.com/repos/plugins/trunk/ATAK_Plugin_Development_Guide.pdf)


### Install ATAK APK on emulator

* Run an emulator
* Paste the ```.apk``` file to ```platform-tools``` in the ```android-sdk``` Linux folder. (often located in ```/usr/lib```)
* Open Terminal and navigate to ```platform-tools``` folder in ```android-sdk```.
* 
    ```./adb install atak.apk```
    
### ATAK documentation via javadoc

In addition to the instructions:

If the javadoc has loaded correctly, you should be able to hover over an ATAK function and get its documentation, e.g. hovering over ```showDropDown``` should bring up a window that includes the description "Produces a dropdown with ..."

Android Studio does not allow this by default, so you may need to go to File -> Settings -> Editor -> General, then check "Show quick documentation on mouse move".

### Notes

Attach debugger to Android process is a debug button in the top right of Android studio

### Make your own plugin

(There's probably a better way to do this, but this works.)

* In ```examples```, ```mkdir <name of your app```
* ```cp -r PluginTemplate/* ../<name of your app```
* In Android Studio, Ctrl+Shift+r Replace occurrences of PluginTemplate, plugintemplate, and Plugin Template with your app name (match upper/lowercase). Rename files accordingly 