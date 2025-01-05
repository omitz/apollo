package com.caci.apollo.speaker_id_library;

import android.Manifest;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import static android.os.SystemClock.sleep;
import static org.junit.Assert.*;
import androidx.test.rule.GrantPermissionRule;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class SpeakerIdTest {
    private static String TAG = "Tommy SpeakerIdTest";

    @Rule
    public GrantPermissionRule permissionRule1 =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO);
    @Rule
    public GrantPermissionRule permissionRule2 =
        GrantPermissionRule.grant(Manifest.permission.READ_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule permissionRule3 =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private static void SaveBitmap (final Bitmap bitmap, final String absFilename) {
        Log.d(TAG, String.format ("Saving %dx%d bitmap to %s.",
                                  bitmap.getWidth(), bitmap.getHeight(), absFilename));
        final File file = new File (absFilename);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress (Bitmap.CompressFormat.PNG, 99, out);
            out.flush();
            out.close();
        } catch (final Exception e) {
            // LOGGER.e(e, "Exception!");
            e.printStackTrace();                
            Log.d (TAG,"SaevBitmap error!");
            System.exit(1);
        }
    }
    


    @Test
    public void useAppContext() {
        // Context of the app under test.
        Log.d (TAG, "testing usingAppContext");
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.caci.apollo.speaker_id_library.test", appContext.getPackageName());
        
    }

    @Test
    public void test1() {

        String param = BuildConfig.TEST_PARAM1;
        Log.d (TAG, "calling test1 with param = " + param);
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // 1.) Create the backend
        SpeakerRecognitionModel spkrID = new SpeakerRecognitionModel ();

        
        // 2.) Load the models
        try {
            if (param.equals ("assets")) {
                spkrID.LoadModels (appContext);
            } else if (param.equals ("data_package")) {
                // For loading models from ATAK data package location
                // (data package must be imported already)
                spkrID.LoadModels (appContext, "ApolloSpeakerID-10VIP");
            } else {
                Log.d (TAG, "invalid param = " + param);
                assertTrue(false);
                return;
            }

            // For loading models from another location (instead of from assets):
            // spkrID.LoadModels (appContext,
            //                    "/sdcard/Download/meta_vosk.pkl",
            //                    "/sdcard/Download/svm_vosk.json",
            //                    "/sdcard/Download/profiles.zip",
            //                    true); // use absolute path
            
        } catch (IOException e) {
            // e.printStackTrace();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Log.d(TAG, sw.toString());
            Log.d (TAG, "Error calling LoadModels");
            return;
        }

        
        // 3.) Get a query wave data and extract the raw audio data
        AssetManager assetManager = appContext.getAssets();
        InputStream query = null;
        short[] rawAudioData = null;
        String wavFile = "testSpkr_mono_16bit_16khz.wav";
        
        try {
            query = assetManager.open (wavFile);
            Log.d (TAG, "qeury size available = " + query.available());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d (TAG, "Error opening query wav file");
            return;
        }
        rawAudioData = spkrID.DecodeAudioData (query, 5); // get at most 5 seconds of raw data

        
        // 4.) Playback the raw audio data
        final boolean[] donePlayback = {false};
        PlaybackThread playbackThread = new PlaybackThread
            (rawAudioData,
             new PlaybackListener() {
                 @Override
                 public void onProgress(int progress) {
                     Log.d (TAG, "playback in progress ");
                 }
                 
                 @Override
                 public void onCompletion() {
                     Log.d (TAG, "done playing back ");
                     donePlayback[0] = true;
                 }
             });
        
        playbackThread.startPlayback();
        while (!donePlayback[0]) {
            Log.d (TAG, "Waiting for playback ");
            sleep (1000);
        }


        // 5.) Compute query embedding and predict the speaker
        SpeakerRecognitionModel.SpeakerInfo spkrInfo;
        spkrInfo = spkrID.PredictSpeaker (rawAudioData);
        Log.d (TAG, "speaker = " + spkrInfo.speakerName);
        Log.d (TAG, "score = " + spkrInfo.maxScore);

        
        // 5.5) Get profile picture for the recognized face
        try {
            Bitmap profileBitmap = spkrID.GetProfilePic (appContext, spkrInfo.speakerName);
            String profileDebug = String.format ("/sdcard/Download/profile_%s.png",
                                                 spkrInfo.speakerName);
            SaveBitmap (profileBitmap, profileDebug);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d (TAG, "Error getting profile picture for " + spkrInfo.speakerName);
            return;
        }
        

        // 6.) Record a 5-second audio
        final boolean[] doneRecording = {false};
        final short[][] recRawAudio = {null};
        RecordingThread recordingThread = new RecordingThread
            (new AudioDataReceivedListener() {
                    @Override
                    public void onAudioDataReceived (short[] data) {
                        /* only gets called if unlimted time recording */
                        Log.d (TAG, "got data len = " + data.length);
                    }
                    
                    @Override
                    public void onAudioDataProgress (int nSamples, long elapsTimeMs) {
                    }

                    @Override
                    public void onAudioDataReady (short[] samples) {
                        Log.d (TAG, "onAudioDataReady len = " + samples.length);
                        doneRecording[0] = true;
                        recRawAudio[0] = samples.clone(); // do we need to make a copy?
                    }
                });
        int recDur = 5;         // record for 5 seconds
        recordingThread.startRecording (recDur); 
        while (!doneRecording[0]) {
            Log.d (TAG, "Waiting for recording ");
            sleep (1000);
            recDur -= 1;
            if (recDur < -1) {
                Log.d (TAG, "Something wrong with recording... ");
                recordingThread.stopRecording ();
                assertTrue(false);
                break;
            }
        }

        
        // 7.) Playback the 5-second audio
        donePlayback[0] = false;
        rawAudioData = recRawAudio[0];
        playbackThread = new PlaybackThread
            (rawAudioData,
             new PlaybackListener() {
                 @Override
                 public void onProgress(int progress) {
                     Log.d (TAG, "playback in progress ");
                 }
                 
                 @Override
                 public void onCompletion() {
                     Log.d (TAG, "done playing back ");
                     donePlayback[0] = true;
                 }
             });
        
        playbackThread.startPlayback();
        while (!donePlayback[0]) {
            Log.d (TAG, "Waiting for playback ");
            sleep (1000);
        }

        
        // 8.) Compute query embedding and predict the speaker
        spkrInfo = spkrID.PredictSpeaker (rawAudioData);
        Log.d (TAG, "speaker = " + spkrInfo.speakerName);
        Log.d (TAG, "score = " + spkrInfo.maxScore);


        // 9.) Test getting AAC audio (can be any data rate)
        String accFile = "Katie_Holmes.aac";
        try {
            query = assetManager.open (accFile);
            Log.d (TAG, "qeury size available = " + query.available());
        } catch (IOException e) {
            e.printStackTrace();
            Log.d (TAG, "Error opening query aac file");
            return;
        }
        rawAudioData = spkrID.DecodeAudioData (query, 5); // get at most 5 seconds of raw data

        
        // 10.) Compute query embedding and predict the speaker
        spkrInfo = spkrID.PredictSpeaker (rawAudioData);
        Log.d (TAG, "speaker = " + spkrInfo.speakerName);
        Log.d (TAG, "score = " + spkrInfo.maxScore);
    }
    
}


