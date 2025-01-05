# Introduction 

ATAK has a subscribe/publish data feed system that allows a plugin to
get real-time data / mission update via TAKServer.  To make this
feature easier to use, we’ve created an API.  Perhaps the best way to
see it in action is to see how the API are used in an example.  In
this example, we have a speakerID analytic.  This analytic is
pre-trained on an particular enrollment dataset consists of 10
celebrities.  The resulting classifier model is loaded from a standard
ATAK data pacakge , which can be manually installed or dynamically
updated from the TAKServer via an ATAK data feed subscription.

Note: To use this Data Feed API, ATAK version 4.3 or above is
required.

# How to compile and install
```
# the following needs to be done just once
./build_and_symlink_latest_datafeed_aar.bash  # need to do only once
./build_and_symlink_latest_speakerid_aar.bash # need to do only once

# now compile the app
./build_apk.bash

# now install the app
adb uninstall com.atakmap.android.helloworld.plugin
adb install atak_helloworld_with_speakerId.apk

```


# Datafeed Configuration

The default datafeed configuration is located in the assets folder:

   helloworld/app/src/main/assets/analytic_model_info.json

In this configuration file, you can set the TAKserver name as well as
the name of the data feed.  Then, you can also identify the Activity
to monitor for model data changes.  Here, the model data are packaged
into an ATAK Data Package format.

For example, the configuration below specifies an analytic named
"SpeakerID" with the corresponding activity named "SpeakerIdActivity".
This means, whenever the model file
"atak_uuid=speakerID_celebrity10.zip" is changed in the data feed, we
will either notify the user or silently replace the model, dependig on
whether the activity, "SpeakerIdActivity", is running or not.

The analytic name can be any unique identifier.  The activity name
will be used to start the activity class defined in a java file.
For example, see SpeakerIdActivity.java.

## analytic_model_info.json example:
```
{
    "comments": "For Tommy's local server, use serverUID= 192.168.1.167:8088:tcp",
    "serverUID": "192.168.1.167:8088:tcp",
    "feedName" : "helloworld",
    "modelsInfo": [
        {
            "analytic": "SpeakerID",
            "activity": "SpeakerIdActivity",
            "dataPackages": [
                {
                    "name": "atak_uuid=speakerID_celebrity10.zip"
                }
            ]
        }
    ]
}
```



# Initialize DataFeed

In order to use DataFeed in your ATAK plugin, you must first modify
`plugin/*Lifecycyle.java` file by adding to `onCreate()` and
`onDestroy()` functions as follows:

## onCreate()

```
    @Override
    public void onCreate(final Activity arg0,
            final transapps.mapi.MapView arg1) {
        //...
        // Finally initialize DataFeedAPI
        DataFeedAPI.init (mapView, pluginContext);
    }
```

## onDestroy()

```
    @Override
    public void onDestroy() {
        //...
        // Finally destroy DataFeedAPI
        DataFeedAPI.dispose();
    }
```


# Starting Activities

In order to have the activity tracked, you need to use `DataFeedAPI.StartActivity()` function in `*DropDownReceiver.java`.  See example `HelloWorldDropDownReceiver.java`:

```
            @Override
            public void onClick(View v) {
                if (DataFeedAPI.HasActivity ("SpeakerIdActivity")) {
                    DataFeedAPI.StartActivity ("SpeakerIdActivity");
                } 
            }
```

# Tracking Activity Lifecycle

In order to keep track of the activity's lifecycle, you must explicity
set the state (by calling SetLifeCycleStateCreate() or
SetLifeCycleStateDestroy) whenever the activity is created (onCreate)
or destroyed (onDestroy).  Please refer to `SpeakerIdActivity.java`
for example.

## onCreate()
```
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
         *  Track the LifeCycle state
         */
        DataFeedUtil.SetLifeCycleStateCreate (this);
    }
```

## onDestroy()
```
    @Override
    protected void onDestroy() {
        super.onDestroy();

       /*
        *  Track the LifeCycle state
        */
        DataFeedUtil.SetLifeCycleStateDestroy (this);
    }
```
