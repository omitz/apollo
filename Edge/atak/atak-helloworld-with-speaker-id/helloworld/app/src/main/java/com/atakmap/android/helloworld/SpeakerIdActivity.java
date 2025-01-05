//package com.caci.apollo.speakeridbackend;
package com.atakmap.android.helloworld;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.atakmap.android.helloworld.plugin.R;
import com.caci.apollo.speaker_id_library.AudioDataReceivedListener;
import com.caci.apollo.speaker_id_library.PlaybackListener;
import com.caci.apollo.speaker_id_library.PlaybackThread;
import com.caci.apollo.speaker_id_library.RecordingThread;
import com.caci.apollo.speaker_id_library.SpeakerRecognitionModel;

import java.io.IOException;
import java.io.InputStream;

//import static android.os.SystemClock.sleep;

public class SpeakerIdActivity extends Activity {
 // public class MainActivity extends Activity {
    private static String TAG = "Tommy MainActivity";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeakerRecognitionModel m_spkrID;
    private short[] m_rawAudioData = null;
    private String[]             m_permissions = new String[] {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO        
    };
    private ImageView m_profilePic;
    
    public static boolean hasPermission (Activity activity, String[] permissionsList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : permissionsList) {
                boolean granted = activity.checkSelfPermission(permission) ==
                    PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    allGranted = false;
                }
            }
            return allGranted;
        } else {
            return true;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker_id);
        m_profilePic = findViewById (R.id.spkrID_profilePic);


        // Check if user has given permission to record audio
        // int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
        //         Manifest.permission.RECORD_AUDIO);
        // if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
        //     ActivityCompat.requestPermissions (this,
        //                                        new String[]{Manifest.permission.RECORD_AUDIO},
        //                                        PERMISSIONS_REQUEST_RECORD_AUDIO);
        // }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions ( m_permissions, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }

        // hack to wait for permission first
        // while (! hasPermission (this, m_permissions)) {
        //     Log.d (TAG, "waiting for storage permission");
        //     sleep (500);        // very bad..
        // }
        if (hasPermission (this, m_permissions))
            init ();
        
    }


    private void init () {
        // 1.) Create the backend
        Context ctx = this;
        m_spkrID = new SpeakerRecognitionModel();

        // 2.) Load the models
        try {
            m_spkrID.LoadModels(ctx);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error calling LoadModels");
            return;
        }

        // 3.) Get a query wave data
        AssetManager assetManager = ctx.getAssets();
        InputStream query = null;

        String aacFile = "Katie_Holmes.aac";
        try {
            query = assetManager.open(aacFile);
            Log.d(TAG, "qeury size available = " + query.available());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "Error opening query aac file");
            return;
        }
        m_rawAudioData = m_spkrID.DecodeAudioData(query, 5); // get at most 5 seconds of raw data


        // Setup Playback button
        Button playback_btn1 = findViewById(R.id.playback_btn1);
        playback_btn1.setOnClickListener
            (new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TextView resultView = findViewById(R.id.result_text);
                        resultView.append ("Started Playingback\n");
                        PlaybackThread playbackThread = new PlaybackThread
                            (m_rawAudioData,
                             new PlaybackListener() {
                                 @Override
                                 public void onProgress(int progress) {
                                     Log.d (TAG, "playback in progress ");
                                 }
                                 
                                 @Override
                                 public void onCompletion() {
                                     Log.d (TAG, "done playing back ");
                                     TextView resultView = findViewById(R.id.result_text);
                                     resultView.append ("Done Playing back\n");
                                 }
                             });
                        playbackThread.startPlayback();
                    }
                }
            );

        
        // Setup record button
        Button recordAudio_btn = findViewById(R.id.recordAudio_btn);
        recordAudio_btn.setOnClickListener
            (new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        TextView resultView = findViewById(R.id.result_text);
                        resultView.append ("Started Recording\n");
                        RecordingThread recordingThread = new RecordingThread
                            (new AudioDataReceivedListener() {
                                    @Override
                                    public void onAudioDataReceived (short[] data) {
                                        /* only gets called if unlimted time recording */
                                        Log.d (TAG, "got data len = " + data.length);
                                    }
                                    
                                    @Override
                                    public void onAudioDataProgress (int nSamples,
                                                                     long elapsTimeMs) {
                                    }
                                    
                                    @Override
                                    public void onAudioDataReady (short[] samples) {
                                        Log.d (TAG, "onAudioDataReady len = " + samples.length);
                                        m_rawAudioData = samples.clone(); // make a copy?

                                        // Stuff that updates the UI
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                TextView resultView = findViewById(R.id.result_text);
                                                resultView.append ("Done Recording\n");
                                            }
                                        });

                                    }
                                }
                            );
                        recordingThread.startRecording (5); // record for 5 seconds
                    }
                }
            );


       Button quit_btn = findViewById(R.id.quit_btn);
       quit_btn.setOnClickListener (new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   finishAndRemoveTask ();
               }
           });
       
        // Setup speakerID button
        Button process_btn = findViewById(R.id.process_btn);
        process_btn.setOnClickListener (new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SpeakerRecognitionModel.SpeakerInfo spkrInfo;
                    spkrInfo = m_spkrID.PredictSpeaker (m_rawAudioData); // blocking
                    Log.d (TAG, "speaker = " + spkrInfo.speakerName);
                    Log.d (TAG, "score = " + spkrInfo.maxScore);
                    
                    TextView resultView = findViewById(R.id.result_text);
                    resultView.append ("speaker = " + spkrInfo.speakerName + "\n");
                    resultView.append ("score = " + spkrInfo.maxScore + "\n");
                    
                    // Get profile picture for the recognized speaker
                    try {
                        Bitmap profileBitmap = m_spkrID.GetProfilePic (ctx, spkrInfo.speakerName);
                        m_profilePic.setImageBitmap (profileBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d (TAG, "Error getting profile picture for " +
                               spkrInfo.speakerName);
                    }
                }
            });
    }        


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // assume always granted
        init ();
    }    
}
