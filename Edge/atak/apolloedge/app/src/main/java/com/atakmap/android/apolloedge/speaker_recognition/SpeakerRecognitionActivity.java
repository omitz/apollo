package com.atakmap.android.apolloedge.speaker_recognition;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.atakmap.android.apolloedge.plugin.R;
import com.caci.apollo.datafeed.DataFeedUtil;
import com.caci.apollo.speaker_id_library.AudioDataReceivedListener;
import com.caci.apollo.speaker_id_library.PlaybackListener;
import com.caci.apollo.speaker_id_library.PlaybackThread;
import com.caci.apollo.speaker_id_library.RecordingThread;
import com.caci.apollo.speaker_id_library.SpeakerRecognitionModel;
import com.caci.apollo.speaker_id_library.spkrIdUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;



public class SpeakerRecognitionActivity extends AppCompatActivity {

    /*************************************
     * Constants 
     ************************************/
    public static final Integer WAVFILE_REQUEST_CODE = 1;
    private static final String TAG = "Tommy SpeakerRecognitionActivity";
    static private final int HOME_VIEW = 1;
    static private final int AUDIO_RECORD_VIEW = 2;
    static private final int RECORDING_UI = 3;
    static private final int REDO_PLAY_PROCESS_UI = 4;
    static private final int PROCESSING_UI = 5;
    static private final int RESULT_SCORE_UI = 6;
    static private final int FILE_IMPORTED_VIEW = 7;
    static private final int MODEL_LOADED = 8;
    static private final int MODEL_NOT_LOADED = 9;
    
    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    
    
    /*************************************
     * member variables
     ************************************/
    private SpeakerRecognitionActivity m_thisActivity;
    private TextView m_resultView;
    private View m_homeView;
    private View m_recordView;

    private RecordingThread   m_RecordingThread;
    private PlaybackThread    m_PlaybackThread;
    private SpeakerRecognitionModel   m_spkrID;
    private short[] m_rawAudioData = null;

    // private ImageView m_profilePic;

    public String[] permissions = new String[] {
        // Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.RECORD_AUDIO,
    };
    
    
    /*************************************
     * data structures
     ************************************/
    public class PreloadModels extends AsyncTask<Void, Void, Exception> {
        WeakReference<SpeakerRecognitionActivity> activityReference;

        PreloadModels(SpeakerRecognitionActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                // m_spkrID.LoadModels (activityReference.get());
                // Try loading data package with uuid="ApolloSpeakerID-10VIP"
                m_spkrID.LoadModels (activityReference.get(), "ApolloSpeakerID-10VIP");
                Log.d (TAG, "model loaded!");
            } catch (IOException e) {
                return e;
            }
            return null;        // no error See onPostExecute()
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) { // load model failed 
                activityReference.get().setErrorState // show error on resultView
                    (String.format(activityReference.get().getString(R.string.failed), result));
            } else {
                // activityReference.get().setUiState(STATE_READY);
                Log.d (TAG, "STATE_READY");
                activityReference.get().setUiState(MODEL_LOADED);
            }
        }
    }
    

    // TC 2021-01-11 (Mon) -- Another task that gets the speakerID
    private class RecognizeSpeakerTask
        extends AsyncTask<Void, Void, SpeakerRecognitionModel.SpeakerInfo> {
        WeakReference<SpeakerRecognitionActivity> activityReference;
        // WeakReference<TextView> resultView;
        short[] audioData = null;
        
        RecognizeSpeakerTask (SpeakerRecognitionActivity activity,
                              // TextView resultView,
                              short[] samples) {
            this.activityReference = new WeakReference<>(activity);
            // this.resultView = new WeakReference<>(resultView);
            this.audioData = samples;
        }

        @Override
        protected SpeakerRecognitionModel.SpeakerInfo doInBackground (Void... params) {
            // SpeakerRecognitionModel.SpeakerInfo spkrInfo;
            return m_spkrID.PredictSpeaker (audioData); // see onPostExecute()
        }

        @Override
        protected void onPostExecute (SpeakerRecognitionModel.SpeakerInfo speakerInfo) {
            // get speaker and score indicator
            TextView speakerName = m_recordView.findViewById(R.id.speakerName);
            TextView simScore = m_recordView.findViewById(R.id.sim_score);
            ImageView profilePic = m_recordView.findViewById (R.id.spkrID_profilePic);
            ConstraintLayout cl = (ConstraintLayout)
                m_recordView.findViewById (R.id.scoreResultLayout);
            ConstraintSet cs = new ConstraintSet();
            cs.clone (cl);

            // no speech --> no match
            if (speakerInfo == null) {
                speakerName.setText ("No Speech Detected");
                cs.setHorizontalBias (R.id.sim_score, (float) 0);
                cs.applyTo(cl);
                simScore.setText("0");
                profilePic.setImageDrawable (null);
                profilePic.setVisibility (View.GONE);
            } else {
                speakerName.setText (speakerInfo.speakerName);
                cs.setHorizontalBias (R.id.sim_score, (float) (speakerInfo.maxScore/100.0));
                cs.applyTo(cl);
                simScore.setText (String.format("%2.0f", speakerInfo.maxScore));

                // Get profile picture for the recognized speaker
                try {
                    Bitmap profileBitmap = m_spkrID.GetProfilePic (m_thisActivity,
                                                                   speakerInfo.speakerName);
                    // m_profilePic.setImageBitmap (profileBitmap);
                    profilePic.setImageBitmap (profileBitmap);
                    profilePic.setVisibility (View.VISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d (TAG, "Error getting profile picture for " +
                           speakerInfo.speakerName);
                }
            }

            // present the views
            activityReference.get().setUiState(RESULT_SCORE_UI);

        }
    }
    

    /*************************************
     * inheritance 
     ************************************/
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        Log.d (TAG, "calling onCreate");
        m_thisActivity = this;
        m_spkrID = new SpeakerRecognitionModel ();

        // Setup layout
        m_homeView = getLayoutInflater().inflate
            (R.layout.activity_speaker_recognition, null);
        m_recordView = getLayoutInflater().inflate
            (R.layout.activity_speaker_recognition_record_audio, null);
        SetupHomeView ();
        SetupRecordView ();
        
        // set home view:
        setUiState(HOME_VIEW);
        setUiState(MODEL_NOT_LOADED);


        // input audio from mic:
        m_RecordingThread = new RecordingThread(new AudioDataReceivedListener() {
            int count = 0;
            final int UPDATE_FREQ = 5;
                
            @Override
            public void onAudioDataReceived(short[] data) {
                Log.d (TAG, "got data len = " + data.length);
            }

            @Override
            public void onAudioDataProgress(int nSamples, long elapsTimeMs) {
                // Log.d (TAG, String.format("nSampls = %d, timeStamp=%d", nSamples, elapsTimeMs));
                // Log.d (TAG, "progress = " + progress);
                ProgressBar recProgress = m_recordView.findViewById(R.id.recording_progressBar);
                int progress = (int) elapsTimeMs;
                count ++;
                if (count > UPDATE_FREQ) {
                    count = 0;
                    recProgress.setProgress(progress);
                    recProgress.postInvalidate();
                }
            }

            @Override
            public void onAudioDataReady(short[] samples) {
                Log.d (TAG, "onAudioDataReady ");

                // m_audioRawData = samples;
                m_rawAudioData = samples.clone(); // do we need to make a copy?

                // Stuff that updates the UI
                runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setUiState(REDO_PLAY_PROCESS_UI);
                        }
                    });
            }
        });

        
        // Check if user has given permission to record audio
        // int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(),
        //                                                         Manifest.permission.RECORD_AUDIO);
        // if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
        //     ActivityCompat.requestPermissions(this,
        //                                       new String[]{Manifest.permission.RECORD_AUDIO},
        //                                       PERMISSIONS_REQUEST_RECORD_AUDIO);
        //     return;
        // }


        if (! hasPermission(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new PreloadModels(this).execute();

        /*
         *  Track the LifeCycle state
         */
        DataFeedUtil.SetLifeCycleStateCreate (this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        /*
         *  Track the LifeCycle state
         */
        DataFeedUtil.SetLifeCycleStateDestroy (this);
    }    
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == WAVFILE_REQUEST_CODE) {
                spkrIdUtils.Assert.that (data != null && data.getData() != null, "");
                    
                // Get the file's content URI from the incoming Intent
                Uri uri = data.getData();

                // get wav data content 
                short[] wavData = null;
                try {
                    InputStream inputStream =
                        getApplicationContext().getContentResolver().openInputStream(uri);
                    assert inputStream != null;
                    wavData = m_spkrID.DecodeAudioData (inputStream,
                                                        5); // get at most 5 seconds of data
                    
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (wavData == null) {
                    m_rawAudioData = null;
                    // invalid wav file format
                    m_resultView.setText("INVALID AUDIO");

                } else {
                    m_rawAudioData = wavData.clone(); // do we need to make a copy?
                    m_resultView.setText("");
                    setUiState (FILE_IMPORTED_VIEW);
                }
                
            } else {
                Log.d (TAG, "Unhandeld requestCode " + requestCode);
                spkrIdUtils.Assert.that (false, "");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                new PreloadModels(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void finish() {
        super.finishAndRemoveTask();
    }
    
    /*************************************
     * helper functions
     ************************************/
    boolean hasPermission(String[] permissionsList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : permissionsList) {
                boolean granted =
                    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    allGranted = false;
                }
            }
            return allGranted;
        } else {
            return true;
        }
    }

    
    private void SetupHomeView () {
        // Setup widgets
        m_resultView = m_homeView.findViewById(R.id.result_text);
        Log.d (TAG, "STATE_START");

        // input audio file:
        m_homeView.findViewById(R.id.inputAudioFile_btn).
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("audio/*");
                    startActivityForResult(intent, WAVFILE_REQUEST_CODE);
                }
            });
        
        m_homeView.findViewById(R.id.recordAudio_btn).
            setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setUiState (AUDIO_RECORD_VIEW);
                }
            });
    }


    private void SetupRecordView () {
        // setup record button:
        Button startRec_btn = m_recordView.findViewById(R.id.startRecording_btn);
        startRec_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d (TAG, "Start Recording");
                    m_resultView.setText("");
                    setUiState(RECORDING_UI);
                    m_RecordingThread.startRecording(5); // record for 5 seconds
                }
            });
        
        // Setup record progress bar
        ProgressBar recProgress = m_recordView.findViewById(R.id.recording_progressBar);
        recProgress.setProgress(0);
        recProgress.setMin(0);
        recProgress.setMax(5000); // 5 seconds
        

        // Setup playback button
        Button playback_btn = m_recordView.findViewById(R.id.playback_btn);
        playback_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spkrIdUtils.Assert.that (m_rawAudioData != null, "");

                    if (m_PlaybackThread == null) {
                        Log.d (TAG, "Playback now");
                        m_PlaybackThread =
                            new PlaybackThread(m_rawAudioData, new PlaybackListener() {
                                    @Override
                                    public void onProgress(int progress) {
                                    }
                                    
                                    @Override
                                    public void onCompletion() {
                                        Log.d (TAG, "Completed playback");
                                        m_PlaybackThread = null;
                                        playback_btn.setText ("Playback");
                                        playback_btn.postInvalidate();
                                    }});
                        m_PlaybackThread.startPlayback();
                        playback_btn.setText ("Stop");
                        playback_btn.postInvalidate();
                    } else {
                        Log.d (TAG, "Playback stopped");
                        m_PlaybackThread.stopPlayback();
                        m_PlaybackThread = null;
                        playback_btn.setText ("Playback");
                        playback_btn.postInvalidate();
                    }
                }
        });


        // Setup process button
        Button process_btn = m_recordView.findViewById(R.id.process_btn);
        process_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d (TAG, "Start Processing");
                    // find the closest speaker
                    spkrIdUtils.Assert.that (m_rawAudioData != null, "");

                    if (playback_btn.getText().equals("Stop"))
                        playback_btn.callOnClick();

                    setUiState(PROCESSING_UI);
                    
                    // new RecognizeSpeakerTask (m_thisActivity, m_resultView,
                    //                           m_rawAudioData).execute();
                    new RecognizeSpeakerTask (m_thisActivity, // m_resultView,
                                              m_rawAudioData).execute();
                }
            });

        // Setup redo button
        Button redo_btn = m_recordView.findViewById(R.id.redo_btn);
        redo_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d (TAG, "Redo from beginning");
                    if (playback_btn.getText().equals("Stop"))
                        playback_btn.callOnClick();
                    setUiState(HOME_VIEW);
                }
            });

        // // Setup profile image
        // m_profilePic = m_recordView.findViewById (R.id.spkrID_profilePic);
    }

    

    private void setRecordViewUIs (int startRec, int progressBar, int redoPlayProcess,
                                   int process_progressBar, int scoreResult) {
        m_recordView.findViewById(R.id.startRecording_btn).setVisibility(startRec);
        m_recordView.findViewById(R.id.recordAudio_lbl).setVisibility(startRec);
        m_recordView.findViewById(R.id.progressBarLayout).setVisibility(progressBar);
        m_recordView.findViewById(R.id.redoPlayProcessLayout).setVisibility(redoPlayProcess);
        m_recordView.findViewById(R.id.process_progressBar).setVisibility(process_progressBar);
        m_recordView.findViewById(R.id.scoreResultLayout).setVisibility(scoreResult);
    }

    private void setUiState(int state) {
        
        switch (state) {
        case MODEL_NOT_LOADED:
            m_recordView.findViewById(R.id.process_btn).setEnabled(false);
            break;
        case MODEL_LOADED:
            m_recordView.findViewById(R.id.process_btn).setEnabled(true);
            TextView modelVerion = m_homeView.findViewById(R.id.speakerid_model_version);
            modelVerion.setText ("Model Version = " + m_spkrID.GetVersion());
            modelVerion = m_recordView.findViewById(R.id.speakerid_model_sig);
            modelVerion.setText ("Model Version = " + m_spkrID.GetVersion());
            break;
        case HOME_VIEW:
            m_homeView.setVisibility(View.VISIBLE);
            m_recordView.setVisibility (View.GONE);
            setContentView(m_homeView);
            break;
        case AUDIO_RECORD_VIEW:
            // change layout to "record audio"
            setRecordViewUIs (View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE);
            // switch to record view:
            m_homeView.setVisibility(View.GONE);
            m_recordView.setVisibility(View.VISIBLE);
            setContentView (m_recordView);
            break;
        case FILE_IMPORTED_VIEW:
            // change layout to "record audio"
            setRecordViewUIs (View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE);
            // switch to record view:
            m_homeView.setVisibility(View.GONE);
            m_recordView.setVisibility(View.VISIBLE);
            setContentView (m_recordView);
            break;
        case RECORDING_UI:
            setRecordViewUIs (View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE);
            break;
        case REDO_PLAY_PROCESS_UI:
            setRecordViewUIs (View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE);
            break;
        case PROCESSING_UI:
            setRecordViewUIs (View.GONE, View.GONE, View.VISIBLE, View.VISIBLE, View.GONE);
            // disable redo and playback
            m_recordView.findViewById(R.id.redo_btn).setEnabled(false);
            m_recordView.findViewById(R.id.playback_btn).setEnabled(false);            
            break;
        case RESULT_SCORE_UI:
            setRecordViewUIs (View.GONE, View.GONE, View.VISIBLE, View.GONE, View.VISIBLE);
            // enable redo and playback
            m_recordView.findViewById(R.id.redo_btn).setEnabled(true);
            m_recordView.findViewById(R.id.playback_btn).setEnabled(true);
            break;
        default:
            throw new IllegalStateException("Unexpected value: " + state);
        }
    }
    
    
    public void setErrorState(String message) {
        m_resultView.setText(message);
    }


    private static String getFilePath(String FILE_NAME, String externalDir) {
        String filePath = externalDir + "/" + FILE_NAME;
        
        Log.d(TAG, "File path: " + filePath);
        return filePath;
    }
    
}
