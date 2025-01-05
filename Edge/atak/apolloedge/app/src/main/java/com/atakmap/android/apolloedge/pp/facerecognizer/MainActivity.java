/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package  com.atakmap.android.apolloedge.pp.facerecognizer;

import android.content.ClipData;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

//import androidx.appcompat.app.AlertDialog;

import com.atakmap.android.apolloedge.plugin.R;
//import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

//import apollo_utils.ApolloFileUtils;
//import apollo_utils.TransferUtils;
//import apollo_utils.UtilsDatetime;
//import apollo_utils.UtilsLocation;
import com.atakmap.android.apolloedge.apollo_utils.*;

import com.atakmap.android.apolloedge.pp.facerecognizer.utils.BitmapConfig;
import com.atakmap.android.apolloedge.pp.facerecognizer.utils.BorderedText;
import com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils;
import com.atakmap.android.apolloedge.pp.facerecognizer.utils.Logger;
import com.atakmap.android.apolloedge.pp.facerecognizer.tracking.MultiBoxTracker;
// import pp.facerecognizer.UtilsAWS;
// import pp.facerecognizer.OverlayView;
// import pp.facerecognizer.Classifier;

import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.ROOT;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.VIPS_SUBDIR;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.saveResultsToJson;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.saveMobileResultToRefJson;

/**
 * Entry point for face recognition; An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class MainActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    protected static final int FACE_SIZE = 160;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;

    private static final String S3_INPUT_DIR = "inputs/face/";

    private static final String DEMO_IMG = "face_demo_img.jpg";

    private Integer sensorOrientation;

    private Classifier classifier;
    private MultiBoxTracker tracker;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;


    private byte[] luminanceCopy;

    private BorderedText borderedText;

    private Snackbar initSnackbar;
    private Snackbar trainSnackbar;
    private Button addToDbButton;
    private Button add_individual;
    private Button saveButton;
    private Button viewResultsButton;
    public boolean SAVE = false;

    private boolean initialized = false;
    private boolean training = false;
    private boolean trained = false;

    private UtilsLocation locUtil;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RelativeLayout container = findViewById(R.id.container);
        initSnackbar = Snackbar.make(container, "Initializing...", Snackbar.LENGTH_INDEFINITE);
        trainSnackbar = Snackbar.make(container, "Training data...", Snackbar.LENGTH_INDEFINITE);

        // Unused feature: Add an individual to the classifier via the phone GUI
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edittext, null);
        EditText editText = dialogView.findViewById(R.id.edit_text);
        AlertDialog editDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.enter_name)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                    int idx = classifier.addPerson(editText.getText().toString());
                    ApolloFileUtils.performFileSearch(this, idx - 1);
                })
                .create();

        // Uncomment after CDR
//        addToDbButton = findViewById(R.id.train_on_vips);
//        addToDbButton.setOnClickListener(view ->
//                performDirSearch());
//        add_individual = findViewById(R.id.add_button);
//        add_individual.setOnClickListener(view ->
//                new AlertDialog.Builder(MainActivity.this)
//                        .setTitle(getString(R.string.select_name))
//                        // set a list of items to be displayed
//                        // i will be the index of the person clicked in the list (if you count "Add new person" as 0)
//                        .setItems(classifier.getClassNames(), (dialogInterface, i) -> {
//                             LOGGER.w("add add_individual onClickListener i: ", i);
//                            // if "Add new person" was clicked
//                            if (i == 0) {
//                                editDialog.show();
//                            // if a known person was clicked
//                            } else {
//                                ApolloFileUtils.performFileSearch(this, i - 1);
//                            }
//                        })
//                        .show());

        saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> SAVE = true);

        viewResultsButton = findViewById(R.id.view_results_button);
        viewResultsButton.setOnClickListener(v ->
        {
            Intent resultsActivity = new Intent(getBaseContext(), ViewResultsActivity.class);
            startActivity(resultsActivity);
        });

        // If the user chooses to save a file, the ideal naming requires location to be on and might require wifi to be on. These checks need to be done on the UI thread
        locUtil = new UtilsLocation(this);
        locUtil.locationAndWifiOn(this);
        locUtil.startListening();
    }

    @Override
    public void onResume() {
        super.onResume();

        // We already requested permissions in the parent onCreate
        if (hasPermission(permissions)) {
            init();
        }

        if (hasPermission(permissions)) {
            // Check if the model has already been trained. (This assumes that if there are any names in the label file, the model has already been trained.)
            ArrayList<String> labels = new ArrayList<>();
            try {
                labels = FileUtils.readLabel(FileUtils.LABEL_FILE);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            if (labels.size() > 0) {
                trained = true;
            }

            if (!trained) {
                performDirSearch();
            }
        }
    }

    @Override
    public void finish() {
        super.finishAndRemoveTask();
    }


    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        if (!initialized)
            new Thread(this::init).start();

        final float textSizePx =
        TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);

        BitmapConfig camBc = new BitmapConfig(size, sensorOrientation);
        rgbFrameBitmap = camBc.getFullsizeBm();
        croppedBitmap = camBc.getCroppedBm();
        frameToCropTransform = camBc.getFrameToCropTransform();
        cropToFrameTransform = camBc.getCropToFrameTransform();

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> {
                    tracker.draw(canvas);
                    if (isDebug()) {
                        tracker.drawDebug(canvas);
                    }
                });

        addCallback(
                // Lambda expression - the general syntax is (Parameters) -> { Body }
                canvas -> {
                    if (!isDebug()) {
                        return;
                    }
                    final Bitmap copy = cropCopyBitmap;
                    if (copy == null) {
                        return;
                    }
                    final int backgroundColor = Color.argb(100, 0, 0, 0);
                    canvas.drawColor(backgroundColor);
                    final Matrix matrix = new Matrix();
                    final float scaleFactor = 2;
                    matrix.postScale(scaleFactor, scaleFactor);
                    matrix.postTranslate(
                            canvas.getWidth() - copy.getWidth() * scaleFactor,
                            canvas.getHeight() - copy.getHeight() * scaleFactor);
                    canvas.drawBitmap(copy, matrix, new Paint());

                    final Vector<String> lines = new Vector<String>();
                    if (classifier != null) {
                        final String statString = classifier.getStatString();
                        final String[] statLines = statString.split("\n");
                        Collections.addAll(lines, statLines);
                    }
                    lines.add("");
                    lines.add("Frame: " + previewWidth + "x" + previewHeight);
                    lines.add("Crop: " + copy.getWidth() + "x" + copy.getHeight());
                    lines.add("View: " + canvas.getWidth() + "x" + canvas.getHeight());
                    lines.add("Rotation: " + sensorOrientation);
                    lines.add("Inference time: " + lastProcessingTimeMs + "ms");

                    borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
                });
    }

    OverlayView trackingOverlay;

    void init() {
        runOnUiThread(()-> initSnackbar.show());
        File dir = new File(ROOT);

        AssetManager mgr = getAssets();
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        // Copy the model files
        String[] assetFiles = new String[]{FileUtils.DATA_FILE, FileUtils.MODEL_FILE, FileUtils.LABEL_FILE};
        for (String assetFile : assetFiles) {
            String destPath = ROOT + File.separator + assetFile;
            String srcFile = FileUtils.ASSETS_SUBDIR + File.separator + assetFile;
            ApolloFileUtils.copyAsset(mgr, srcFile, destPath);
        }
        // Copy the vips mobile dataset (same as apollo/apollo/Command/vip/vips)
        String onDeviceVipsDir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
                FileUtils.VIPS_SUBDIR;
        File onDeviceVipsDirFile = new File(onDeviceVipsDir);
        if (!onDeviceVipsDirFile.isDirectory()) {
            onDeviceVipsDirFile.mkdirs();
        }
        try {
            String[] personDirs = mgr.list(FileUtils.VIPS_ASSET_PATH);
            // Make the person dir on the device
            for (String personDir : personDirs) {
                File onDevicePersonDir = new File(onDeviceVipsDir + File.separator + personDir);
                if (!onDevicePersonDir.isDirectory()) {
                    onDevicePersonDir.mkdirs();
                }
                // Copy all of that person's images over
                String personDirFullpath = FileUtils.VIPS_ASSET_PATH + File.separator + personDir;
                String[] images = mgr.list(personDirFullpath);
                for (String img: images) {
                    String srcFile = personDirFullpath + File.separator + img;
                    String destPath =
                            onDeviceVipsDir + File.separator +
                            personDir + File.separator +
                            img;
                    ApolloFileUtils.copyAsset(mgr, srcFile, destPath);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            classifier = Classifier.getInstance(mgr, FACE_SIZE, FACE_SIZE);
        } catch (Exception e) {
            LOGGER.e("Exception initializing classifier! finish", e);
            finish();
        }
        runOnUiThread(()-> initSnackbar.dismiss());
        initialized = true;
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();
        tracker.onFrame(
                previewWidth,
                previewHeight,
                getLuminanceStride(),
                sensorOrientation,
                originalLuminance,
                timestamp);
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection || !initialized || training) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
//         LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            FileUtils.saveBitmap(croppedBitmap, "preview.jpeg", (Location) null);
        }

        runInBackground(
                    () -> {
                        // LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

                        List<Classifier.Recognition> mappedRecognitions =
                                classifier.recognizeImage(croppedBitmap,cropToFrameTransform);
                        // For debugging, visualize croppedBitmap
                        // FileUtils.saveBitmap(croppedBitmap, "croppedBitmap.jpeg");

                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        // Display the boxes and predictions
                        tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
                        trackingOverlay.postInvalidate();

                        // See the current camera
                        requestRender();
                        computingDetection = false;

                        Location globalGeoloc = null;

                        if (SAVE) {

                            globalGeoloc = locUtil.requestLoc(globalGeoloc);

                            // create unique name
                            String uid = "";
                            if (globalGeoloc != null) {
                                uid += String.format("lat_%s_long_%s_", globalGeoloc.getLatitude(), globalGeoloc.getLongitude());
                            }
                            uid += UtilsDatetime.createDatetimeStr(this);
                            final String fName = String.format("%s.jpeg", uid);

                            // create matrix with which to transform the coordinates
                            Matrix rgbMatrix = new Matrix();
                            rgbMatrix.setRotate(sensorOrientation);
                            Bitmap save = Bitmap.createBitmap(rgbFrameBitmap,
                                    0,
                                    0,
                                    rgbFrameBitmap.getWidth(),
                                    rgbFrameBitmap.getHeight(),
                                    rgbMatrix,
                                    true);
                            String originalFilepath = FileUtils.saveBitmap(save, fName, globalGeoloc);

                            String fullFilename = ROOT + File.separator + fName; //eg /storage/emulated/0/facerecognizer/lat_38.8803871_long_-77.1366618_2020-11-18T14-22-01-0500.jpeg
                            TransferUtils.queueUploadViaApi(this, fullFilename, S3_INPUT_DIR, null, false);

                            Canvas drawing = new Canvas(save);
                            mappedRecognitions = tracker.drawMapped(drawing, mappedRecognitions);

                            final String drawingName = String.format("%s_mobile_result.jpeg", uid);
                            String mobileResultFilepath = FileUtils.saveBitmap(save, drawingName, globalGeoloc);
                            String jsonResultFilepath = saveResultsToJson(mappedRecognitions, uid, fName);
                            Toast.makeText(this, "Image Saved", Toast.LENGTH_LONG).show();
                            SAVE = false;
                            saveMobileResultToRefJson(fName, drawingName, jsonResultFilepath);
                        }
                    });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        /*
        This function gets called by the Android system when the activity finished
        data: the intent that called the activity in the first place, e.g. Intent { dat=content://com.android.externalstorage.documents/document/primary:face/1571174443.png flg=0x43 }
         */
        if (!initialized) {
            Snackbar.make(
                    getWindow().getDecorView().findViewById(R.id.container),
                    "Try it again later", Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }

        if (resultCode == RESULT_OK) {
            trainSnackbar.show();
            add_individual.setEnabled(false);
            training = true;

            ClipData clipData = data.getClipData();
            // Create an array of the image uris for the person
            ArrayList<Uri> uris = new ArrayList<>();

            if (clipData == null) {
                uris.add(data.getData());
            } else {
                for (int i = 0; i < clipData.getItemCount(); i++)
                    uris.add(clipData.getItemAt(i).getUri());
            }

            startTrainingThread(requestCode, uris);

        }
    }

    private void startTrainingThread(int requestCode, ArrayList<Uri> uris) {
        /*
        requestCode: the integer label for the person
         */
        new Thread(() -> {
            try {
                classifier.updateData(requestCode, getContentResolver(), uris);
            } catch (Exception e) {
                LOGGER.e(e, "Exception!");
            } finally {
                training = false;
            }
            runOnUiThread(() -> {
                trainSnackbar.dismiss();
                add_individual.setEnabled(true);
            });
        }).start();
    }

    /**
     * Iterate over the vips directory on the phone, adding each person to the training data for the classifier and retraining it.
     */
    public void performDirSearch() {

        runOnUiThread(() -> {
            trainSnackbar.show();
        });

        runInBackground(() -> {
            String dirStr = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + VIPS_SUBDIR;
            File dir = new File(dirStr);
            File[] personDirs = dir.listFiles();

            // Get the list of people the classifier has already been trained on
            CharSequence[] classNames = classifier.getClassNames();
            List<String> classStrs = new ArrayList<>();
            for (CharSequence name: classNames) {
                classStrs.add(name.toString());
            }

            // Guard against user not following README
            if (personDirs != null) {

                for(File person_dir : personDirs) {
                    // Get just the person's name from the file
                    String[] pathList = person_dir.toString().split("/");
                    String person = pathList[pathList.length-1];

                    // If the classifier hasn't been trained on this person yet
                    if (!classStrs.contains(person)) {

                        // Create an array of the image uris for the person
                        ArrayList<Uri> uris = new ArrayList<>();
                        // For now, we're limiting this to 10 images per person
                        int numImgs;
                        File[] personsFiles = person_dir.listFiles();
                        // Guard against user not following README
                        if (personsFiles != null) {
                            numImgs = personsFiles.length;
                            Log.i("SVM", "Processing " + numImgs + " files for person");
                            File[] firstFiles = new File[numImgs];
                            for (int i = 0; i < numImgs; i++) {
                                firstFiles[i] = personsFiles[i];
                            }
                            for(File img : firstFiles) {
                                uris.add(Uri.fromFile(img));
                            }

                            int idx = classifier.addPerson(person) - 1;
                            LOGGER.w(person, idx);
                            training = true;
                            try {
                                classifier.updateData(idx, getContentResolver(), uris);
                            } catch (Exception e) {
                                LOGGER.e(e, "Exception!");
                            }
                        }
                    }
                }
            }
            trainSnackbar.dismiss();
            training = false;
            trained = true;
        });
    }
}
