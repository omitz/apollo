// Copyright 2019 Alpha Cephei Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.atakmap.android.apolloedge.speech_to_text;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.apolloedge.plugin.R;

import org.kaldi.Assets;
import org.kaldi.KaldiRecognizer;
import org.kaldi.Model;
import org.kaldi.RecognitionListener;
import org.kaldi.SpeechService;
import org.kaldi.Vosk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

public class KaldiActivityV2 extends Activity implements
        RecognitionListener {

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_DONE = 2;
    static private final int STATE_FILE = 3;
    static private final int STATE_MIC  = 4;
    static private final int SPACING = 10;
    private static final String TAG = "Tommy Speech to Text V2";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;


    private Model model;
    private SpeechService speechService;
    TextView resultView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_kaldi);

        // disable file button
        findViewById(R.id.recognize_file).setVisibility(View.GONE);

        // Setup layout
        resultView = findViewById(R.id.result_text);
        setUiState(STATE_START);

        findViewById(R.id.recognize_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognizeFile();
            }
        });

        findViewById(R.id.recognize_mic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recognizeMicrophone();
            }
        });

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        RadioGroup langRadioGroup = findViewById(R.id.languageGroup);
        RadioButton engRadioButton = findViewById(R.id.englishBtn);
        langRadioGroup.check(R.id.englishBtn);
        // new SetupTask(this).execute("english");
        engRadioButton.callOnClick();
    }


    private void switchLanguage (String lang) {
        setUiState(STATE_START);
        new SetupTask(this).execute(lang);
        // Toast.makeText(
        //     applicationContext, "$lang Selected",
        //     Toast.LENGTH_SHORT
        // ).show()
        Toast.makeText (getApplicationContext(), lang + " Selected", Toast.LENGTH_SHORT).show();
//        transcribeIntentAudioFile()
    }

    public void englishBtnHandler(View v) {
        switchLanguage("english");
    }

    public void russianBtnHandler(View v) {
        switchLanguage("russian");
    }

    public void spanishBtnHandler(View v) {
        switchLanguage("spanish");
    }

    public void frenchBtnHandler(View v) {
        switchLanguage("french");
    }

    
    
    private static class SetupTask extends AsyncTask<String, Void, Exception> {
        WeakReference<KaldiActivityV2> activityReference;

        SetupTask(KaldiActivityV2 activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(String... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                Log.d("KaldiDemo", "Sync files in the folder " + assetDir.toString());

                Vosk.SetLogLevel(0);

                assert params[0] != null;
                String lang = params[0];
                Log.d (TAG, "lang is " + lang);
                if (lang.equals("english")) {
                    Log.d (TAG, "set to english");
                    activityReference.get().model =
                        new Model(assetDir.toString() + "/model-android-en");
                } else if (lang.equals("russian")) {
                    Log.d (TAG, "set to russian");
                    activityReference.get().model =
                        new Model(assetDir.toString() + "/model-android-ru");
                } else if (lang.equals("french")) {
                    Log.d (TAG, "set to french");
                    activityReference.get().model =
                        new Model(assetDir.toString() + "/model-android-fr");
                } else if (lang.equals("spanish")) {
                    Log.d (TAG, "set to spanish");
                    activityReference.get().model =
                        new Model(assetDir.toString() + "/model-android-es");
                }

            } catch (IOException e) {
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception result) {
            if (result != null) {
                activityReference.get().setErrorState(String.format(activityReference.get().getString(R.string.failed), result));
            } else {
                activityReference.get().setUiState(STATE_READY);
            }
        }
    }

    private static class RecognizeTask extends AsyncTask<Void, Void, String> {
        WeakReference<KaldiActivityV2> activityReference;
        WeakReference<TextView> resultView;

        RecognizeTask(KaldiActivityV2 activity, TextView resultView) {
            this.activityReference = new WeakReference<>(activity);
            this.resultView = new WeakReference<>(resultView);
        }

        @Override
        protected String doInBackground(Void... params) {
            KaldiRecognizer rec;
            long startTime = System.currentTimeMillis();
            StringBuilder result = new StringBuilder();
            try {
                rec = new KaldiRecognizer(activityReference.get().model, 16000.f, "[\"oh zero one two three four five six seven eight nine\"]");

                InputStream ais = activityReference.get().getAssets().open("10001-90210-01803.wav");
                if (ais.skip(44) != 44) {
                    return "";
                }
                byte[] b = new byte[4096];
                int nbytes;
                while ((nbytes = ais.read(b)) >= 0) {
                    if (rec.AcceptWaveform(b, nbytes)) {
                        result.append(rec.Result());
                    } else {
                        result.append(rec.PartialResult());
                    }
                }
                result.append(rec.FinalResult());
            } catch (IOException e) {
                return "";
            }
            return String.format(activityReference.get().getString(R.string.elapsed), result.toString(), (System.currentTimeMillis() - startTime));
        }

        @Override
        protected void onPostExecute(String result) {
            activityReference.get().setUiState(STATE_READY);
            Log.d (TAG, "result is = " + result);
            resultView.get().append(result + "\n");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                // new SetupTask(this).execute();
                switchLanguage("english");
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (speechService != null) {
            speechService.cancel();
            speechService.shutdown();
        }
    }

    @Override
    public void finish() {
        super.finishAndRemoveTask();
    }

    @Override
    public void onResult(String hypothesis) {

        Log.d (TAG, hypothesis + "\n");
        int ix_begin = hypothesis.lastIndexOf("\"text\" :");
        int ix_end = hypothesis.lastIndexOf("\"\n}");
        Log.d (TAG, "ix_begin = " + ix_begin);
        Log.d (TAG, "ix_end = " + ix_end);
        String sub =  hypothesis.substring(ix_begin + SPACING, ix_end);
        Log.d (TAG, "sub = '" + sub + "'\n");
        if (sub != "") {
            resultView.append("Vocal transcription: \n" + sub + "\n");
        }
        // // resultView.append(hypothesis + "\n");
    }

    @Override
    public void onPartialResult(String hypothesis) {
        // resultView.append(hypothesis + "\n");
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        speechService.cancel();
        speechService = null;
        setUiState(STATE_READY);
    }


    private void setLanguageRadioButtonState(Boolean state) {
        findViewById(R.id.englishBtn).setEnabled(state);
        findViewById(R.id.russianBtn).setEnabled(state);
        findViewById(R.id.frenchBtn).setEnabled(state);
        findViewById(R.id.spanishBtn).setEnabled(state);
    }


    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                resultView.setText(R.string.preparing);
                resultView.setMovementMethod(new ScrollingMovementMethod());
                findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.recognize_mic).setEnabled(false);
                setLanguageRadioButtonState(false);
                break;
            case STATE_READY:
                resultView.setText(R.string.ready);
                //((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                ((Button) findViewById(R.id.recognize_mic)).
                                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_rec, 0, 0, 0);
                findViewById(R.id.recognize_file).setEnabled(true);
                findViewById(R.id.recognize_mic).setEnabled(true);
                setLanguageRadioButtonState(true);
                break;
            case STATE_DONE: //Stop recording
                //((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                ((Button) findViewById(R.id.recognize_mic)).
                                    setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_rec, 0, 0, 0);
                findViewById(R.id.recognize_file).setEnabled(true);
                findViewById(R.id.recognize_mic).setEnabled(true);
                setLanguageRadioButtonState(true);

                /* check to see if nothing is said */
                if (resultView.getText().toString().equals (getString(R.string.listening))) {
                    resultView.setText(R.string.ready);
                }
                break;
            case STATE_FILE:
                resultView.setText(getString(R.string.starting));
                findViewById(R.id.recognize_mic).setEnabled(false);
                findViewById(R.id.recognize_file).setEnabled(false);
                setLanguageRadioButtonState(false);
                break;
            case STATE_MIC: //start to record
                //Button bt  = ((Button) findViewById(R.id.recognize_mic)).setText(R.string.stop_microphone);
                Button bt  = ((Button) findViewById(R.id.recognize_mic));
                resultView.setText(R.string.listening);
                findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.recognize_mic).setEnabled(true);
                bt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_stop, 0, 0, 0);
                setLanguageRadioButtonState(false);
                break;
        }
    }

    private void setErrorState(String message) {
        resultView.setText(message);
        ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
        findViewById(R.id.recognize_file).setEnabled(false);
        findViewById(R.id.recognize_mic).setEnabled(false);
    }

    public void recognizeFile() {
        setUiState(STATE_FILE);
        new RecognizeTask(this, resultView).execute();
    }

    public void recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE);
            speechService.cancel();
            speechService = null;
        } else {
            setUiState(STATE_MIC);
            try {
                KaldiRecognizer rec = new KaldiRecognizer(model, 16000.0f);
                speechService = new SpeechService(rec, 16000.0f);
                speechService.addListener(this);
                speechService.startListening();
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

}
