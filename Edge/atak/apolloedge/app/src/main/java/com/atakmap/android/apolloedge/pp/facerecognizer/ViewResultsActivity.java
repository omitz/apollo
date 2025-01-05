package  com.atakmap.android.apolloedge.pp.facerecognizer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.atakmap.android.apolloedge.apollo_utils.ApolloFileUtils;
import com.atakmap.android.apolloedge.apollo_utils.NetworkTask;
import com.atakmap.android.apolloedge.login.LoginActivity;
import com.atakmap.android.apolloedge.plugin.R;
import com.atakmap.android.apolloedge.apollo_utils.TransferUtils;
import com.atakmap.android.apolloedge.pp.facerecognizer.tracking.MultiBoxTracker;
import com.atakmap.android.apolloedge.pp.facerecognizer.utils.BitmapConfig;
import com.atakmap.android.apolloedge.pp.facerecognizer.utils.ColorThresholds;
import com.atakmap.android.apolloedge.pp.facerecognizer.utils.ExifTags;
import com.google.android.material.tabs.TabLayout;

import com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils;

import static com.atakmap.android.apolloedge.apollo_utils.ApolloConstants.S3_INPUT_DIR;
import static com.atakmap.android.apolloedge.pp.facerecognizer.MainActivity.FACE_SIZE;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.GeolocUtils.convertDMSToDegree;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.COMMAND;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.MOBILE;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.REF_JSON;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.ROOT;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.readJsonObjectFromFile;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.saveBitmap;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.saveCommandResultToRefJson;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.saveResultsToJson;
import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.FileUtils.saveMobileResultToRefJson;

public class ViewResultsActivity extends AppCompatActivity implements NetworkTask.AsyncResponse {

    private static final int NUM_COLUMNS = 3;
    private int IMPORT_IMG = 1234;

    private RVAdapter mAdapter;
    private RecyclerView mNumbersList;
    private Button importButton;
    private Button syncButton;

    private Classifier classifier;
    private MultiBoxTracker tracker;

    // imageIds is a list of integers that indicate where in the grid layout the image at that index will go
    private List<Integer> imageIds = new ArrayList<>();
    private List<String> imagePaths = new ArrayList<>();
    private List<Bitmap> imageBitmaps = new ArrayList<>();

    TabLayout tabLayout;
    TabLayout.BaseOnTabSelectedListener tabListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_results);
        try {
            parseDir();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        setUpTabs();

        mNumbersList = (RecyclerView) findViewById(R.id.recycler_view_imgs);

        // Visually divide items
        mNumbersList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));
        mNumbersList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), NUM_COLUMNS);
        mNumbersList.setLayoutManager(layoutManager);
        renderResults();

        importButton = findViewById(R.id.importButton);
        importButton.setOnClickListener(v -> {
            ApolloFileUtils.performFileSearch(this, IMPORT_IMG);
        });

        syncButton = findViewById(R.id.sync_button);
        syncButton.setOnClickListener(v -> {
            Toast.makeText(this, "Syncing...", Toast.LENGTH_LONG).show();
            clearParseAndRender();
        });
    }

    private void setUpTabs() {
        tabLayout = findViewById(R.id.tabLayout);
        tabListener = new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                clearParseAndRender();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        };
        tabLayout.addOnTabSelectedListener(tabListener);
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.apollo_blue1));
    }

    private void clearParseAndRender() {
        try {
            imagePaths.clear();
            imageIds.clear();
            imageBitmaps.clear();
            parseDir();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        renderResults();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
        tracker = new MultiBoxTracker(this);
    }

    void init() {
        AssetManager mgr = getAssets();
        try {
            classifier = Classifier.getInstance(mgr, FACE_SIZE, FACE_SIZE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderResults() {
        ArrayList<ImageData> createLists = prepareResultsDataForDisplay();
        mNumbersList.setHasFixedSize(true);
        mAdapter = new RVAdapter(getApplicationContext(), createLists);
        mNumbersList.setAdapter(mAdapter);
    }

    private ArrayList<ImageData> prepareResultsDataForDisplay() {
        ArrayList<ImageData> imagesData = new ArrayList<>();
        for (int i = 0; i < imageIds.size(); i++) {
            ImageData imageData = new ImageData();
            imageData.setImageID(imageIds.get(i));
            imageData.setImagePath(imagePaths.get(i));
            imageData.setImageBitmap(imageBitmaps.get(i));
            imagesData.add(imageData);
        }
        return imagesData;
    }

    /**
     * Iterate over the reference json to add the appropriate results to imagePaths, etc
     * @throws IOException, JSONException
     */
    private void parseDir() throws IOException, JSONException {
        // Determine if the user is tabbed to see Edge results or Command results
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        int tabPosition = tabLayout.getSelectedTabPosition();
        String mobileOrCommand;
        if (tabPosition == 0) {
            mobileOrCommand = MOBILE;
        } else {
            mobileOrCommand = COMMAND;
        }
        // Get all of the result filepaths from the reference json
        File refJson = new File(REF_JSON);
        JSONObject refJsonObj = readJsonObjectFromFile(refJson);
        Iterator<String> keys = refJsonObj.keys();
        List<String> mobileOrCommandResults = new ArrayList<>();
        while (keys.hasNext()) {
            String key = keys.next();
            Object objValue = refJsonObj.get(key);
            String value = objValue.toString();
            JSONObject jsonChildObject = new JSONObject(value);
            try {
                String resultBasename = jsonChildObject.get(mobileOrCommand).toString();
                mobileOrCommandResults.add(resultBasename);
            } catch (JSONException e) {
                if (mobileOrCommand.equals(COMMAND)) {
                    // We don't have the Command result, so we'll a) send the job and b) check the image server for the Command result
                    // Send the job (in case it wasn't sent before)
                    String ogFilepath = ROOT + File.separator + key;
                    TransferUtils.queueUploadViaApi(this, ogFilepath, S3_INPUT_DIR, this, true);
                    // Check the image server for the Command result
                    // Infer S3 path //TODO Have the image server path come from the json response when the image was sent (instead of inferring it here)
                    String cmdBasename = jsonChildObject.get(MOBILE).toString().replace(MOBILE, COMMAND).replace("jpeg", "png");
                    String cmdS3Key = "outputs/face/" + cmdBasename;
                    // Get the image from the cantaloupe image server
                    String urlStr = "https://images.apollo-cttso.com/iiif/2/" + cmdS3Key.replace("/", "%2F").replace("+", "%2B") + "/full/full/0/default.jpg";
                    new DownloadImageTask(key, cmdBasename).execute(urlStr);
                    // TODO Running these jobs multiple times (bc all mobile jobs are sent with ignore hash True) may flood the Command side databases. Consider changing the Command file-hash analytic to hash based on file contents AND name, and then sending mobile jobs with ignore hash False.
                }
            }
        }
        sortNewestToOldest(mobileOrCommandResults);
        for (int i = 0; i < mobileOrCommandResults.size(); i++) {
            // Add the mobile result
            String path = ROOT + File.separator + mobileOrCommandResults.get(i);
            imagePaths.add(path);
            imageIds.add(i);
            File file = new File(path);
            String filepathStr = String.valueOf(file);
            Bitmap bitmap = BitmapFactory.decodeFile(filepathStr);
            imageBitmaps.add(bitmap);
        }
    }

    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        String ogBasename;
        String cmdBasename;
        public DownloadImageTask(String ogBasename, String cmdBasename) {
            this.ogBasename = ogBasename;
            this.cmdBasename = cmdBasename;
        }
        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            URL url = null;
            try {
                url = new URL(urldisplay);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Bitmap bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            try {
                assert url != null;
                InputStream is = (InputStream) url.getContent();
                bitmap = BitmapFactory.decodeStream(is);
                // The Command face recognition analytic saves a png, but on Android we use jpeg so that we can use ExifInterface
                cmdBasename = cmdBasename.replace("png", "jpeg");
                saveBitmap(bitmap, cmdBasename, (Location) null);
                saveCommandResultToRefJson(ogBasename, cmdBasename);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap bitmap) {
            Log.i("ViewResultsActivity", "onPostExecute");
        }
    }

    private void sortNewestToOldest(List<String> mobileOrCommandResults) {
        // Sort them by last modified date (newest at top)
        mobileOrCommandResults.sort(new Comparator<String>() {
            @Override
            // https://stackoverflow.com/questions/26582070/sorting-files-based-on-its-creation-date-in-android
            public int compare(String o1, String o2) {
                File fullpath1 = new File(ROOT, o1);
                File fullpath2 = new File(ROOT, o2);
                long diff = fullpath2.lastModified() - fullpath1.lastModified();
                if (diff > 0) {
                    return 1;
                } else if (diff == 0) {
                    return 0;
                } else {
                    return -1;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // If a user selected an image from Import Image Button.
        if (requestCode == IMPORT_IMG && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri imageUri = data.getData();
                processExternalImage(imageUri);
                Log.i("ViewResultsActivity", "calling clearParseAndRender");
                clearParseAndRender();
            } else {
                Log.d("BAD_IMG", "no image given un proper format!");
            }
            return;
        }
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

                    String imageUrigetPath = imageUri.getPath();
                    File externalImgFile = new File(imageUrigetPath);
                    final String[] split = externalImgFile.getPath().split(":");
                    String filepath = Environment.getExternalStorageDirectory() + File.separator + split[1]; //TODO: This assumes that the imported file is coming from /storage/emulated/0. This has not been tested on true external storage. Test if this will work with SD card
                    String id = "";
                    // Get metadata (both to use in the filename and to save to the bitmap copy)
                    ExifInterface exifInterface = null;
                    ExifTags exifTags = null;
                    try {
                        try {
                            exifInterface = new ExifInterface(imageUrigetPath);
                        } catch (FileNotFoundException e) {
                            // Handle case where we don't get a full file path from ACTION_OPEN_DOCUMENT/the file is not in ExternalStorageDirectory
                            InputStream in = getContentResolver().openInputStream(imageUri);
                            exifInterface = new ExifInterface(in);
                            in.close();
                        }
                        // https://stackoverflow.com/questions/5269462/how-do-i-convert-exif-long-lat-to-real-values
                        String latStr = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                        String latRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                        String longStr = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                        String longRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
                        exifTags = new ExifTags(latStr, latRef, longStr, longRef);
                        double latitude = convertDMSToDegree(latStr);
                        double longitude = convertDMSToDegree(longStr);
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
                        Log.i("ViewResultsActivity", "unable to read datetime from " + filepath);
                    }

                    if (id.length() == 0) {
                        // Create unique name (Since we don't know where or when this image was taken, we won't follow the geoloc/datetime naming convention)
                        id = UUID.randomUUID().toString();
                    }

                    final String fName = String.format("%s.jpeg", id);
                    // Copy to new file in the facerecognizer dir
                    String originalFilepath = saveBitmap(externalBitmap, fName, exifTags);

                    String fullFilename = ROOT + File.separator + fName;
                    TransferUtils.queueUploadViaApi(this, fullFilename, S3_INPUT_DIR, this, true);

                    Bitmap mutableBitmap = externalBitmap.copy(Bitmap.Config.ARGB_8888, true);
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

                    final String drawingName = String.format("%s_mobile_result.jpeg", id);
                    String mobileResultFilepath = saveBitmap(mutableBitmap, drawingName, exifTags);
                    String jsonResultFn = saveResultsToJson(mappedRecognitions, id, fName);
                    saveMobileResultToRefJson(fName, drawingName, jsonResultFn);
                }).start();
    }

    // Override NetworkTaskUpload's AsyncResponse method (NetworkTaskUpload is used in queueUploadViaApi)
    @Override
    public void processFinish(String uploadHttpRequestResult) {
        // Receive the result of onPostExecute
        if (uploadHttpRequestResult != null) {
            if (uploadHttpRequestResult.equals("error")) {
                // Open the login activity
                Intent loginIntent = new Intent(getBaseContext(), LoginActivity.class);
                startActivity(loginIntent);
            }
        }
    }

    @Override
    public void onDestroy() {
        tabLayout.removeOnTabSelectedListener(tabListener);
        super.onDestroy();
    }
}
