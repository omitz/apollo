package com.atakmap.android.apolloedge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.apolloedge.auth.AuthProvider;
import com.atakmap.android.apolloedge.plugin.R;

import com.atakmap.android.toolbar.widgets.TextContainer;

import com.atakmap.comms.NetConnectString;
import com.atakmap.android.contact.Connector;
import com.atakmap.android.contact.Contact;
import com.atakmap.android.contact.Contacts;
import com.atakmap.android.contact.IndividualContact;
import com.atakmap.android.contact.IpConnector;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.caci.apollo.datafeed.DataFeedAPI;

import java.lang.*;
import java.util.*;


/**
 * The DropDown Receiver should define the visual experience that a
 * user might have while using this plugin.  At a basic level, the
 * dropdown can be a view of your own design that is inflated.  Please
 * be wary of the type of context you use.  As noted in the Map
 * Component, there are two contexts - the plugin context and the atak
 * context.  When using the plugin context - you cannot build thing or
 * post things to the ui thread.  You use the plugin context to lookup
 * resources contained specifically in the plugin.
 *
 * Mission Package Support:  (Taken from ATAK Confluence pages)
 *
 *        - "For a client to receive updates, a call must be made to
 *          explicitly subscribe."
 *
 *        - "Explicitly unsubscribing from updates is also
 *          supported. In addition, when a client disconnects, any
 *          mission subscriptions for that client will be
 *          automatically cancelled, which can be considered an
 *          "implicit unsubscribe".
 *
 *        - "After the subscribe call is processed by the server (which should
 *          be nearly instantaneous), the broker will deliver mission change
 *          CoT messages."
 *
 *        - "Once a client has subscribed to a mission, it will
 *          receive CoT notifications of changes to the mission. The
 *          "mission change" CoT messages will be send to the client
 *          UID for which a subscriptions has been registered."
 *
 *        - "Subscriptions to missions are tracked (in-memory) by the
 *            TAK Server core services, on a "per *client UID*"
 *            basis. For a client to receive updates, a call must be
 *            made to explicitly subscribe."
 *
 *        - "After the subscribe call is processed by the server
 *            (which should be nearly instantaneous), the broker will
 *            deliver mission change CoT messages."
 *
 *        - "client can check for any mission invitations sent while
 *            they were offline"
 */
public class ApolloEdgeDropDownReceiver extends DropDownReceiver
    implements OnStateListener, SensorEventListener {


    /**************************** Member Constants *****************************/
    public static final String SHOW_PLUGIN = "com.atakmap.android.apolloedge.SHOW_PLUGIN";
    private static final String TAG = "Tommy ApolloEdgeDropDownReceiver";


    /**************************** Member Variables *****************************/
    private View pluginView;
    private final Context pluginContext;

    /**************************** CONSTRUCTOR *****************************/

    public ApolloEdgeDropDownReceiver(final MapView mapView,
                                      final Context context,
                                      ApolloEdgeMapOverlay overlay) {
        super(mapView);
        this.pluginContext = context;

        Log.d (TAG, "Calling ApolloEdgeDropDownReceiver constructor");
        
        // If you are using a custom layout you need to make use of
        // the PluginLayoutInflator to clear out the layout cache so
        // that the plugin can be properly unloaded and reloaded.
        pluginView = PluginLayoutInflater.inflate (pluginContext, R.layout.plugin_layout, null);

        // **** Setup GUI Elements:  ****
        Log.d (TAG, "Setting up GUI elements");

        final Button login_btn = pluginView.findViewById(R.id.login_btn);
        login_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName ("com.atakmap.android.apolloedge.plugin",
                        "com.atakmap.android.apolloedge.login.LoginActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this shows a new task
                getMapView().getContext().startActivity(intent);
            }
        });

        final Button logout_btn = pluginView.findViewById(R.id.logout_btn);
        logout_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                ApolloEdgeDropDownReceiver.this.logOut();
            }
        });


        // The new speakerID v2
        final Button speakerIDv2_btn = pluginView.findViewById(R.id.speakerIDv2_btn);
        speakerIDv2_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String activityName = "speaker_recognition.SpeakerRecognitionActivity";
                if (DataFeedAPI.HasActivity (activityName)) {
                    DataFeedAPI.StartActivity (activityName);
                } else {
                    Intent intent = new Intent();
                    intent.setClassName
                        ("com.atakmap.android.apolloedge.plugin",
                         "com.atakmap.android.apolloedge." + activityName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this shows a new task
                    getMapView().getContext().startActivity(intent);
                }
            }
        });
        

        final Button speechToText_btn = pluginView.findViewById(R.id.speechToText_btn);
        speechToText_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName
                    ("com.atakmap.android.apolloedge.plugin",
                     "com.atakmap.android.apolloedge.speech_to_text.KaldiActivityV2");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this shows a new task
                getMapView().getContext().startActivity(intent);
            }
        });

        final Button ocr_btn = pluginView.findViewById(R.id.ocr_btn);
        ocr_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName ("com.atakmap.android.apolloedge.plugin",
                                     "com.atakmap.android.apolloedge.ocr.OCRActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this shows a new task
                getMapView().getContext().startActivity(intent);
            }
        });

        final Button faceRec_btn = pluginView.findViewById(R.id.faceRec_btn);
        faceRec_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName ("com.atakmap.android.apolloedge.plugin",
                        "com.atakmap.android.apolloedge.pp.facerecognizer.MainActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this shows a new task
                getMapView().getContext().startActivity(intent);
            }
        });

        final Button faceRecV2_btn = pluginView.findViewById(R.id.faceRecV2_btn);
        faceRecV2_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClassName ("com.atakmap.android.apolloedge.plugin",
                        "com.atakmap.android.apolloedge.face_recognition.FaceRecognitionActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this shows a new task
                getMapView().getContext().startActivity(intent);
            }
        });
        
        final Button upload_btn = pluginView.findViewById(R.id.upload_btn);
        upload_btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView login_info_text = pluginView.findViewById(R.id.login_error_text);
                login_info_text.setVisibility(View.VISIBLE);
            }
        });

        final Button login_info_button = pluginView.findViewById(R.id.login_info_btn);
        login_info_button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView login_info_text = pluginView.findViewById(R.id.login_error_text);
                login_info_text.setVisibility(View.VISIBLE);
            }
        });

    }


    /************************* Helper Methods *************************/

    private NetConnectString getIpAddress(IndividualContact ic) {
        Connector ipConnector = ic.getConnector(IpConnector.CONNECTOR_TYPE);
        if (ipConnector != null) {
            String connectString = ipConnector.getConnectionString();
            return NetConnectString.fromString(connectString);
        } else {
            return null;
        }

    }

    public void logOut() {
        AuthProvider authProvider = new AuthProvider(pluginContext);
        authProvider.logout();
        Activity activity = (Activity) getMapView().getContext();
        activity.startActivity(activity.getIntent());
    }



    /**************************** INHERITED METHODS *****************************/

    @Override
    public void disposeImpl() {
        TextContainer.getTopInstance().closePrompt();
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null)
            return;

        // Show drop-down
        switch (action) {
            case SHOW_PLUGIN:
                showDropDown(pluginView, HALF_WIDTH, FULL_HEIGHT,
                             FULL_WIDTH, HALF_HEIGHT, false, this);
                setAssociationKey("apolloedgePreference");
                List<Contact> allContacts = Contacts.getInstance().getAllContacts();
                for (Contact c : allContacts) {
                    if (c instanceof IndividualContact)
                        Log.d(TAG, "Contact IP address: " + getIpAddress((IndividualContact) c));
                }
                break;
        }
    }


    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            final float[] values = event.values;
            // Movement
            float x = values[0];
            float y = values[1];
            float z = values[2];

            float asr = (x * x + y * y + z * z)
                    / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
            if (Math.abs(x) > 6 || Math.abs(y) > 6 || Math.abs(z) > 8)
                Log.d(TAG, "gravity=" + SensorManager.GRAVITY_EARTH +
                      " x=" + x + " y=" + y + " z=" + z + " asr=" + asr);
            if (y > 7) {
                TextContainer.getTopInstance().displayPrompt("Tilt Right");
                Log.d(TAG, "tilt right");
            } else if (y < -7) {
                TextContainer.getTopInstance().displayPrompt("Tilt Left");
                Log.d(TAG, "tilt left");
            } else if (x > 7) {
                TextContainer.getTopInstance().displayPrompt("Tilt Up");
                Log.d(TAG, "tilt up");
            } else if (x < -7) {
                TextContainer.getTopInstance().displayPrompt("Tilt Down");
                Log.d(TAG, "tilt down");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            Log.d(TAG, "accuracy for the accelerometer: " + accuracy);
        }
    }
    

}
