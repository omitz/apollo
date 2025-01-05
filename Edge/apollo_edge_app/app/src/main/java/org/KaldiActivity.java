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

package org;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.kaldi.Assets;
import org.kaldi.KaldiRecognizer;
import org.kaldi.Model;
import org.kaldi.RecognitionListener;
import org.kaldi.SpeechRecognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import pp.facerecognizer.R;

public class KaldiActivity extends AppCompatActivity implements
        RecognitionListener {

    static {
        System.loadLibrary("kaldi_jni");
    }

    static private final int STATE_START = 0;
    static private final int STATE_READY = 1;
    static private final int STATE_FILE = 2;
    static private final int STATE_MIC  = 3;
    static private final int SPACING = 10;

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Model model;
    private SpeechRecognizer recognizer;
    TextView resultView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.main);

        // Setup layout
        resultView = findViewById(R.id.result_text);
        setUiState(STATE_START);

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

        // disable file button
        findViewById(R.id.recognize_file).setVisibility(resultView.INVISIBLE);
        
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        // new SetupTask(this).execute();
        RadioGroup langRadioGroup = (RadioGroup) findViewById(R.id.languageGroup);
        RadioButton engRadioButton = (RadioButton) findViewById(R.id.englishBtn);
        langRadioGroup.check(R.id.englishBtn);
        engRadioButton.callOnClick();
    }

    public void transcribeIntentAudioFile () {
        // Only set RECOGNIZE FILE if there is a file to recognize.
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("isScanned")) {
            String inputPath;
            inputPath = extras.getString("filePath");
            recognizeFile(inputPath);
        }
    }
    
    public void switchLanguage (String lang) {
        setUiState(STATE_START);
        new SetupTask(this).execute(lang);
        Toast.makeText(getApplicationContext(), lang + " Selected",
                       Toast.LENGTH_SHORT).show();
        transcribeIntentAudioFile ();
    }


    public void englishBtnHandler (View v) {
        switchLanguage ("english");
    }
    
    public void russianBtnHandler (View v) {
        switchLanguage ("russian");
    }

    public void spanishBtnHandler (View v) {
        switchLanguage ("spanish");
    }

    public void frenchBtnHandler (View v) {
        switchLanguage ("french");
    }
    

    
    private static class SetupTask extends AsyncTask<String, Void, Exception> {
        WeakReference<KaldiActivity> activityReference;

        SetupTask(KaldiActivity activity) {
            this.activityReference = new WeakReference<>(activity);
        }

        @Override
        protected Exception doInBackground(String... params) {
            try {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                Log.d("!!!!", assetDir.toString());
                switch (params[0]) {
                case "english":
                    activityReference.get().model = new Model(assetDir.toString() +
                                                              "/model-android-en");
                    break;
                case "russian":
                    activityReference.get().model = new Model(assetDir.toString() +
                                                              "/model-android-ru");
                    break;
                case "french":
                    activityReference.get().model = new Model(assetDir.toString() +
                                                              "/model-android-fr");
                    break;
                case "spanish":
                    activityReference.get().model = new Model(assetDir.toString() +
                                                              "/model-android-es");
                    break;
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

    private static class RecognizeTask extends AsyncTask<String, Void, String> {
        WeakReference<KaldiActivity> activityReference;
        WeakReference<TextView> resultView;

        RecognizeTask(KaldiActivity activity, TextView resultView) {
            this.activityReference = new WeakReference<>(activity);
            this.resultView = new WeakReference<>(resultView);
        }

        @Override
        protected String doInBackground(String... params) {
            KaldiRecognizer rec;
            long startTime = System.currentTimeMillis();
            StringBuilder result = new StringBuilder();
            try {
                rec = new KaldiRecognizer(activityReference.get().model, 16000.f);

                InputStream ais =  new FileInputStream(params[0]);
                
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
            int ix_text = result.lastIndexOf("\"text\" :");
            int ix_end = result.lastIndexOf("\" }");
            String sub = result.substring(ix_text+SPACING, ix_end);
            resultView.get().append("File transcription: \n" + sub + "\n"); //OR resultView.get().append(result + "\n");
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
                new SetupTask(this).execute();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }


    @Override
    public void onResult(String hypothesis) {
        int ix_text = hypothesis.lastIndexOf("\"text\" :");
        int ix_end = hypothesis.lastIndexOf("\" }");
        String sub = hypothesis.substring(ix_text+SPACING, ix_end);
        if (!sub.equals("")){
            resultView.append("Vocal transcription: \n" + sub + "\n"); //OR resultView.append("hypothesis + "\n");
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        resultView.append(""); //OR resultView.append(hypothesis + "\n");
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
        recognizer.cancel();
        recognizer = null;
        setUiState(STATE_READY);
    }

    private void setLanguageRadioButtonState (Boolean state) {
        findViewById(R.id.englishBtn).setEnabled(state);
        findViewById(R.id.russianBtn).setEnabled(state);
        findViewById(R.id.frenchBtn).setEnabled(state);
        findViewById(R.id.spanishBtn).setEnabled(state);
    }
    
    
    private void setUiState(int state) {
        switch (state) {
            case STATE_START:
                resultView.setText(R.string.preparing);
                findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.recognize_mic).setEnabled(false);
                setLanguageRadioButtonState (true);
                break;
            case STATE_READY:
                resultView.setText(R.string.ready);
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
                resultView.setText("");
                findViewById(R.id.recognize_file).setEnabled(true);

                /* if we have intent file, we disable microphone button */
                Bundle extras = getIntent().getExtras();
                if (extras != null && extras.getBoolean("isScanned"))
                    findViewById(R.id.recognize_mic).setEnabled(false);
                else
                    findViewById(R.id.recognize_mic).setEnabled(true);
                
                setLanguageRadioButtonState (true);
                break;
            case STATE_FILE:
                resultView.append(getString(R.string.starting));
                findViewById(R.id.recognize_mic).setEnabled(false);
                findViewById(R.id.recognize_file).setEnabled(false);
                setLanguageRadioButtonState (false);
                break;
            case STATE_MIC:
                ((Button) findViewById(R.id.recognize_mic)).setText(R.string.stop_microphone);
                resultView.setText(R.string.listening);
                findViewById(R.id.recognize_file).setEnabled(false);
                findViewById(R.id.recognize_mic).setEnabled(true);
                setLanguageRadioButtonState (false);
                break;
        }
    }

    private void setErrorState(String message) {
        resultView.setText(message);
        ((Button) findViewById(R.id.recognize_mic)).setText(R.string.recognize_microphone);
        findViewById(R.id.recognize_file).setEnabled(false);
        findViewById(R.id.recognize_mic).setEnabled(false);
    }

    public void recognizeFile(String filePath) {
        setUiState(STATE_FILE);
        String [] inputData = {filePath};
        new RecognizeTask(this, resultView).execute(inputData);
    }

    public void recognizeMicrophone() {
        if (recognizer != null) {
            setUiState(STATE_READY);
            recognizer.cancel();
            recognizer = null;
        } else {
            setUiState(STATE_MIC);
            try {
                recognizer = new SpeechRecognizer(model);
                recognizer.addListener(this);
                recognizer.startListening();
            } catch (IOException e) {
                setErrorState(e.getMessage());
            }
        }
    }

}
