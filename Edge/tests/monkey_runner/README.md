# Monkey Runner

https://developer.android.com/studio/test/monkeyrunner

https://stackoverflow.com/questions/9159223/monkey-tool-in-android-for-mouse-event-injections


## Setup monkeyrunner Jypthon
Assuming your Android Studio is installed at `~/android-studio`.

Setup the java enviroment:
```
export JAVA_HOME=~/android-studio/jre/
export PATH=$JAVA_HOME/jre/bin:$PATH
```

## Running Monkey Recorder
You will get a interactive python session when you run
```
$ monkeyrunner
```


## Using Monkey Recorder
### Start the recorder session:

Start Android emulator or real hone (connect via USB).
```
$ monkeyrunner ./monkey_recorder.py
```
Export actions and save them to `recorded_session.txt`


### Playback the recorded session:
```
$ monkeyrunner ./monkey_playback.py recorded_session.txt
```


### Convert recorded session to jython program:
```
$ python ./monkey_converter.py recorded_session.txt recorded_session.py
$ monkeyrunner recorded_session.py
```



