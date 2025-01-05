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

package pp.facerecognizer;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.media.ExifInterface;
import android.media.ImageReader.OnImageAvailableListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import apollo_utils.ApolloFileUtils;
import apollo_utils.TransferUtils;
import apollo_utils.UtilsDatetime;
import apollo_utils.UtilsLocation;
import pp.facerecognizer.utils.BitmapConfig;
import pp.facerecognizer.utils.BorderedText;
import pp.facerecognizer.utils.ColorThresholds;
import pp.facerecognizer.utils.FileUtils;
import pp.facerecognizer.utils.Logger;
import pp.facerecognizer.tracking.MultiBoxTracker;

import static pp.facerecognizer.utils.FileUtils.ROOT;


/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class MainActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private static final int FACE_SIZE = 160;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);

    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;

    public static final String S3_INPUT_DIR = "inputs/face/";

    private static final String DEMO_IMG = "face_demo_img.jpg";

    private Integer sensorOrientation;

    private Classifier classifier;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;
    private int IMPORT_IMG = 1234;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private byte[] luminanceCopy;

    private BorderedText borderedText;

    private Snackbar initSnackbar;
    private Snackbar trainSnackbar;
    private Button addToDbButton;
    private Button add_individual;
    private Button saveButton;
    private Button importButton;
    private Button viewResultsButton;
    public boolean SAVE = false;

    private boolean initialized = false;
    private boolean training = false;
    private boolean trained = false;

    private Location globalGeoloc;
    private UtilsLocation locUtil;

    private boolean virusBitmapScanned = false;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LOGGER.w("about to start network service");

        RelativeLayout container = findViewById(R.id.container);
        initSnackbar = Snackbar.make(container, "Initializing...", Snackbar.LENGTH_INDEFINITE);
        trainSnackbar = Snackbar.make(container, "Training data...", Snackbar.LENGTH_INDEFINITE);

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

        importButton = findViewById(R.id.importButton);
        importButton.setOnClickListener(v -> {
            ApolloFileUtils.performFileSearch(this, IMPORT_IMG);
        });

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

        if (!virusBitmapScanned) {
            // Check if we are receiving a file from SCANNER!
            Bundle extras = getIntent().getExtras();
            String uriString;
            if (extras != null && extras.getBoolean("isScanned")) {
                uriString = extras.getString("uri");
                Uri imageUri = Uri.parse(uriString);
                tracker = new MultiBoxTracker(this);
                processExternalImage(imageUri);
            }
        }
        virusBitmapScanned = true;
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

            // Copy the model files
            String[] assetFiles = new String[]{FileUtils.DATA_FILE, FileUtils.MODEL_FILE, FileUtils.LABEL_FILE};
            for (String assetFile : assetFiles) {
                String destPath = ROOT + File.separator + assetFile;
                String srcFile = FileUtils.ASSETS_SUBDIR + File.separator + assetFile;
                ApolloFileUtils.copyAsset(mgr, srcFile, destPath);
            }
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

        // Copy the demo image to the device
        String destPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + DEMO_IMG;
        File destFile = new File(destPath);
        if (!destFile.exists()) {
            String srcFile = FileUtils.ASSETS_SUBDIR + File.separator + DEMO_IMG;
            ApolloFileUtils.copyAsset(mgr, srcFile, destPath);
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
            FileUtils.saveBitmap(croppedBitmap, "preview.png");
        }

        runInBackground(
                    () -> {
                        // LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

                        List<Classifier.Recognition> mappedRecognitions =
                                classifier.recognizeImage(croppedBitmap,cropToFrameTransform);
                        // For debugging, visualize croppedBitmap
                        // FileUtils.saveBitmap(croppedBitmap, "croppedBitmap.png");

                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                        // Display the boxes and predictions
                        tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
                        trackingOverlay.postInvalidate();

                        // See the current camera
                        requestRender();
                        computingDetection = false;

                        if (SAVE) {

                            globalGeoloc = locUtil.requestLoc(globalGeoloc);

                            // create unique name
                            String uid = "";
                            if (globalGeoloc != null) {
                                uid += String.format("lat_%s_long_%s_", globalGeoloc.getLatitude(), globalGeoloc.getLongitude());
                            }
                            uid += UtilsDatetime.createDatetimeStr(this);

                            // saveBitmap
                            final String fName = String.format("%s.png", uid);

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
                            FileUtils.saveBitmap(save, fName);

                            String fullFilename = ROOT + File.separator + fName; //eg /storage/emulated/0/facerecognizer/lat_38.8803871_long_-77.1366618_2020-11-18T14-22-01-0500.png
                            TransferUtils.queueUploadViaApi(this, fullFilename, S3_INPUT_DIR);
                            Log.i("MainActivity fullFilename", fullFilename);

                            Canvas drawing = new Canvas(save);
                            mappedRecognitions = tracker.drawMapped(drawing, mappedRecognitions);

                            final String drawingName = String.format("%s_mobile_result.png", uid);
                            FileUtils.saveBitmap(save, drawingName);
                            saveResultsToJson(mappedRecognitions, uid, fName);
                            SAVE = false;
                        }
                    });
    }

    protected void processExternalImage(Uri imageUri) {

        new Thread(
                () -> {
                    Bitmap externalBitmap = null;
                    try {
                        externalBitmap = FileUtils.getBitmapFromUri(this.getContentResolver(), imageUri);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    // Create the config for the bitmap
                    Size size = new Size(externalBitmap.getWidth(), externalBitmap.getHeight());
                    BitmapConfig extBc = new BitmapConfig(size, 0);
                    // An empty fullsize bitmap is created in the BitmapConfig constructor, but we still need to assign the externalBitmap image to the canvas.
                    final Canvas canvas = new Canvas(extBc.getCroppedBm());
                    canvas.drawBitmap(externalBitmap, extBc.getFrameToCropTransform(), null);

                    List<Classifier.Recognition> mappedRecognitions =
                            classifier.recognizeImage(extBc.getCroppedBm(), extBc.getCropToFrameTransform());

                    File externalImgFile = new File(imageUri.getPath());
                    final String[] split = externalImgFile.getPath().split(":");
                    String filepath = Environment.getExternalStorageDirectory() + File.separator + split[1]; //TODO: This assumes that the imported file is coming from /storage/emulated/0. This has not been tested on true external storage. Test if this will work with SD card
                    String id = "";
                    // Get metadata
                    try {
                        ExifInterface exifInterface = new ExifInterface(filepath);
                        // https://stackoverflow.com/questions/5269462/how-do-i-convert-exif-long-lat-to-real-values
                        String latStr = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                        String latRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                        String longStr = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                        String longRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
                        Float latitude = convertToDegree(latStr);
                        Float longitude = convertToDegree(longStr);
                        if (latRef.equals("S")) {
                            latitude *= -1;
                        }
                        if (longRef.equals("W")) {
                            longitude *= -1;
                        }
                        latStr = String.valueOf(latitude);
                        longStr = String.valueOf(longitude);
                        id += String.format("lat_%s_long_%s", latStr, longStr);
                    } catch (IOException | NullPointerException e) {
                        e.printStackTrace();
                    }
                    try {
                        BasicFileAttributes attr = Files.readAttributes(new File(filepath).toPath(), BasicFileAttributes.class);
                        FileTime creationTime = attr.creationTime();
                        if (id.length() > 0) { // If we got the exif data
                            id += "_";
                        }
                        // Edit datetime to match format in processImage(). Docs indicate Z means UTC +0
                        String datetime = String.valueOf(creationTime).replace("Z", "+0000");
                        datetime = datetime.replace(":", "-");
                        id += datetime;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (id.length() == 0) {
                        // Create unique name (Since we don't know where or when this image was taken, we won't follow the geoloc/datetime naming convention)
                        id = UUID.randomUUID().toString();
                    }

                    // Save the original bitmap to the facerecognizer dir
                    final String fName = String.format("%s.png", id);
                    FileUtils.saveBitmap(externalBitmap, fName);

                    String fullFilename = ROOT + File.separator + fName;
                    TransferUtils.queueUploadViaApi(this, fullFilename, S3_INPUT_DIR);

                    Bitmap mutableBitmap = externalBitmap.copy(Config.ARGB_8888, true);
                    Canvas drawing = new Canvas(mutableBitmap);

                    for (Classifier.Recognition recog : mappedRecognitions) {
                        final RectF pos = recog.getLocation();

                        Paint boxPaint = new Paint();
                        boxPaint.setColor(ColorThresholds.setColorBasedOnConfidence(recog.getConfidence()));
                        boxPaint.setStyle(Paint.Style.STROKE);
                        boxPaint.setStrokeCap(Paint.Cap.ROUND);
                        boxPaint.setStrokeJoin(Paint.Join.ROUND);
                        boxPaint.setStrokeMiter(100);
                        boxPaint.setStrokeWidth(4);
                        tracker.drawRecognition(drawing, recog, pos, boxPaint);
                    }
                    final String drawingName = String.format("%s_mobile_result.png", id);
                    FileUtils.saveBitmap(mutableBitmap, drawingName);
                    saveResultsToJson(mappedRecognitions, id, fName);

                }).start();

    }

    private Float convertToDegree(String stringDMS) {
        Float result = null;
        String[] DMS = stringDMS.split(",", 3);
        double deg = divide(DMS[0]);
        double min = divide(DMS[1]);
        double sec = divide(DMS[2]);
        result = (float) (deg + (min / 60) + (sec / 3600));
        return result;
    }

    private double divide(String str) {
        String[] strArr = str.split("/", 2);
        Double num = Double.valueOf(strArr[0]);
        Double den = Double.valueOf(strArr[1]);
        double floatRes = num/den;
        return floatRes;
    }

    private void saveResultsToJson(List<Classifier.Recognition> mappedRecognitions, String uid, String fName) {
        JSONObject resultsJson = new JSONObject();
        try {
            JSONArray faces = new JSONArray();
            for (int i = 0; i < mappedRecognitions.size(); i++) {
                JSONObject face = new JSONObject();
                Classifier.Recognition recog = mappedRecognitions.get(i);
                face.put("confidence", recog.getConfidence());
                face.put("bottom", recog.getLocation().bottom);
                face.put("left", recog.getLocation().left);
                face.put("right", recog.getLocation().right);
                face.put("top", recog.getLocation().top);
                face.put("title", recog.getTitle());
                faces.put(face);
            }
            resultsJson.put(fName, faces);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String fNameResults = String.format("%s.json", uid);
        final File file = new File(ROOT, fNameResults);

        try {
            Writer output;
            output = new BufferedWriter(new FileWriter(file));
            output.write(resultsJson.toString());
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        // If a user selected an image from Import Image Button.
        if (requestCode == IMPORT_IMG && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                processExternalImage(imageUri);
            }
            else {
                Log.d("BAD_IMG", "no image given un proper format!");
            }
            return;
        }

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

    public void performDirSearch() {

        runOnUiThread(() -> {
            trainSnackbar.show();
        });

        runInBackground(() -> {
            String dirStr = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "vips";
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
                            if (personsFiles.length >= 10) {
                                numImgs = 10;
                            } else {
                                numImgs = personsFiles.length;
                            }
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
