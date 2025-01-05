## About

This is a demonstration for Kaldi on Android

## Current Status and Random Thoughts:

 - As of 2020-03-08, we are using testing the latest version.  See
   README_original.md.  This version now supports all 3 platforms:
   X86_64, ARM_32, and ARM_64.
 - It is not clear what model/dataset is used.
 - If we need better speech recognition accuracy, we can try different
   pre-trained model.
 - Different languages are available.
 - There are many tunable pareamte (beam search, etc), but we should
   just use the default values unless there is an performance issue.


## Usage


### Assumptions

As of 2019-10-25, it is assumed that you have the latest android
studio installed (Version 3.5.1).  If not, you may need to modify
'buildme32.bash'.

### Build Instruction

#### Step 1:
```bash
source ./buildme32.bash
```

#### Step 2:

Simply import the project into Android Studio and run. It will listen
for the audio and dump the transcription.

#### Step 3 (optional):
When createing your **new** application, simply modify the demo
according to your needs:

 - add kaldi-android aar to dependencies,
 - update the model and 
 - modify java UI code accodring to your needs.

