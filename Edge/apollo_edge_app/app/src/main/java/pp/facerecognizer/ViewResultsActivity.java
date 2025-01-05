package pp.facerecognizer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import apollo_utils.TransferUtils;
import pp.facerecognizer.utils.FileUtils;

import static pp.facerecognizer.utils.FileUtils.ROOT;

public class ViewResultsActivity extends AppCompatActivity {

    private static final int NUM_COLUMNS = 3;

    private RVAdapter mAdapter;
    private RecyclerView mNumbersList;
    private Button syncButton;

    private List<String> imageTitles = new ArrayList<>();
    // imageIds is a list of integers that indicate where in the grid layout the image at that index will go
    private List<Integer> imageIds = new ArrayList<>();
    private List<String> imagePaths = new ArrayList<>();
    private List<Bitmap> imageBitmaps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_results);

        try {
            parseDir();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        mNumbersList = (RecyclerView) findViewById(R.id.recycler_view_imgs);

        // Visually divide items
        mNumbersList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));
        mNumbersList.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getApplicationContext(), NUM_COLUMNS);
        mNumbersList.setLayoutManager(layoutManager);
        renderResults();

        syncButton = findViewById(R.id.sync_button);
        syncButton.setOnClickListener(v -> {
            try {
                imageTitles.clear();
                imagePaths.clear();
                imageIds.clear();
                imageBitmaps.clear();
                parseDir();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            renderResults();
        });
    }

    private void renderResults() {
        ArrayList<CreateList> createLists = prepareData();
        mNumbersList.setHasFixedSize(true);
        mAdapter = new RVAdapter(getApplicationContext(), createLists);
        mNumbersList.setAdapter(mAdapter);
    }

    private ArrayList<CreateList> prepareData() {
        ArrayList<CreateList> theimage = new ArrayList<>();
        for (int i = 0; i < imageTitles.size(); i++) {
            CreateList createList = new CreateList();
            String filename = imageTitles.get(i);
            // TODO This title display method depends on a naming convention set elsewhere. Potential better solutions include getting this info from a file (json, etc) stored on the phone
            String title = "";
            String[] titleParts = filename.split("_");
            String datetime = null;
            // The standard case where we have lat, long, and a timestamp
            if (Arrays.asList(titleParts).contains("lat")) {
                title += String.format("Lat: %s\nLong: %s\n", titleParts[1], titleParts[3]);
                datetime = titleParts[4];
            } else if (filename.length() >= 1) { // If there wasn't lat long in the filename // >=1 because if we don't have the command result yet, the title for that blank space is "" (as set in loadOrCreateBitmap)
                String datetimeOrUUID = titleParts[0];
                // Check if the first part of the title is a timestamp or a UUID (which occurs with an imported image)
                if (datetimeOrUUID.charAt(10) == "T".charAt(0)) { // If it's a timestamp
                    datetime = datetimeOrUUID;
                } else {
                    title = titleParts[0];
                }
            }
            // If there was a timestamp in the filename, split it so that the date and time will be on separate lines
            if (datetime != null) {
                String[] datetimeParts = datetime.split("T");
                String date = datetimeParts[0];
                String timeAndZone = datetimeParts[1];
                int beginningOfZone = timeAndZone.length() - 5;
                String time = timeAndZone.substring(0, beginningOfZone);
                String timezone = timeAndZone.substring(beginningOfZone);
                title += String.format("Date: %s\nTime: %s\nZone: %s", date, time, timezone);
            }

            createList.setImageTitle(title);
            createList.setImageID(imageIds.get(i));
            createList.setImagePath(imagePaths.get(i));
            createList.setImageBitmap(imageBitmaps.get(i));
            theimage.add(createList);
        }
        return theimage;
    }

    private void parseDir() throws IOException, JSONException {
        File facerecogDir = new File(pp.facerecognizer.utils.FileUtils.ROOT);
        File[] files = facerecogDir.listFiles();
        List<File> mobileResults = new ArrayList<>();
        List<File> originalImages = new ArrayList<>();
        if (files.length > 0) {
            // Iterate over the facerecognizer dir
            for (File file : files) {
                String[] parts = file.toString().split(File.separator);
                String filename = parts[parts.length - 1];
                // Each saved result will have a json file
                if (filename.endsWith("json")) {
                    // Parse the json file to get the original image filename
                    // https://medium.com/@nayantala259/android-how-to-read-and-write-parse-data-from-json-file-226f821e957a
                    FileReader fileReader = new FileReader(file);
                    BufferedReader bufferedReader = new BufferedReader(fileReader);
                    StringBuilder stringBuilder = new StringBuilder();
                    String line = bufferedReader.readLine();
                    while (line != null) {
                        stringBuilder.append(line).append("\n");
                        line = bufferedReader.readLine();
                    }
                    bufferedReader.close();
                    String response = stringBuilder.toString();
                    JSONObject jsonObject = new JSONObject(response);
                    String key = String.valueOf(jsonObject.keys().next());
                    Log.i("ViewResults keys", key);
                    originalImages.add(new File(ROOT + File.separator + key));
                    String basename;
                    if (key.contains(".")) {
                        basename = key.substring(0, key.lastIndexOf('.'));
                    } else {
                        basename = key;
                    }
                    String mobileResult = basename + "_mobile_result.png";
                    mobileResults.add(new File(FileUtils.ROOT, mobileResult));
                }
            }
            sortNewestToOldest(mobileResults);
            sortNewestToOldest(originalImages);

            for (int i = 0; i < mobileResults.size(); i++) {
                // Add the mobile result
                String filestr = mobileResults.get(i).getName();
                imageTitles.add(filestr);
                imagePaths.add(String.valueOf(mobileResults.get(i)));
                /**
                 * Add the layout index for the image.
                 * e.g. in a 3 column layout
                 +-----------------------------------------------+
                 |  image id 0   |  image id 1   |  image id 2   |
                 +-----------------------------------------------+
                 |  image id 3   |  image id 4   |  image id 5   |
                 +-----------------------------------------------+
                 */
                imageIds.add(i*NUM_COLUMNS);
                File filepath = new File(facerecogDir, filestr);
                String filepathStr = String.valueOf(filepath);
                Bitmap bitmap = BitmapFactory.decodeFile(filepathStr);
                imageBitmaps.add(bitmap);

                String originalLocalFile = String.valueOf(originalImages.get(i));
                // Now add the facenet command result
                String cmdResult = filestr.replace("mobile", "command");
                int position = (i*NUM_COLUMNS)+1;
                loadOrCreateBitmap(facerecogDir, originalLocalFile, cmdResult, position);
                // Put the command result (or the blank space) to the right of the mobile result
                imageIds.add(position);

                // Now add the face resnet command result
                String resnetCmdResult = filestr.replace("mobile", "resnet_command");
                position = (i*NUM_COLUMNS)+2;
                loadOrCreateBitmap(facerecogDir, originalLocalFile, resnetCmdResult, position);
                // Put the command result (or the blank space) to the right of the facenet command result
                imageIds.add(position);
            }
        }
    }

    private void sortNewestToOldest(List<File> mobileResults) {
        // Sort them by last modified date (newest at top)
        Collections.sort(mobileResults, new Comparator<File>() {
            @Override
            // https://stackoverflow.com/questions/26582070/sorting-files-based-on-its-creation-date-in-android
            public int compare(File o1, File o2) {
                long diff = o2.lastModified() - o1.lastModified();
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

    /**
     * If we have the Command result, add it to the list of bitmaps.
     * @param cmdResult What the filename of the Command result will be (if it exists)
     */
    private void loadOrCreateBitmap(File facerecogDir, String fullLocalFilepath, String cmdResult, Integer position) {
        File cmdPath = new File(facerecogDir, cmdResult);
        String cmdPathStr = String.valueOf(cmdPath); // eg /storage/emulated/0/facerecognizer/lat_38.9253437_long_-77.2045246_2020-07-22T16-22-25-0400_command_result.png
        Log.i("ViewResults cmdPath: ", cmdPathStr);
        if (cmdPath.exists()) {
            Log.i("ViewResults", cmdPathStr + " exists!");
            imageTitles.add(cmdResult);
            imagePaths.add(cmdPathStr);
            Bitmap cmdBitmap = BitmapFactory.decodeFile(String.valueOf(cmdPath));
            imageBitmaps.add(cmdBitmap);
        } else {
            if (cmdPathStr.contains("resnet_command")) {
                // We removed the face_resnet service, so we no longer expect this result from Command.
                // We'll leave the empty space to help explain that this pipeline is not limited to a single Command model.
                imageTitles.add("");
                imagePaths.add("");
                Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                imageBitmaps.add(emptyBitmap);
            } else {
                Log.i("ViewResults", cmdPathStr + " does not exist");
                // In case the user clicked 'SAVE' when they had no internet connection, send the upload again
                TransferUtils.queueUploadViaApi(this, fullLocalFilepath, MainActivity.S3_INPUT_DIR);
                // If there's no command result, have a blank space
                imageTitles.add("");
                imagePaths.add("");
                Bitmap emptyBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                imageBitmaps.add(emptyBitmap);
                // Get the Command result
                // Infer S3 path //TODO this is fragile.. maybe the path should instead come from the json response when the job was sent?
                String[] pathEls = cmdPathStr.split("/");
                String cmdBasename = pathEls[pathEls.length - 1];
                String cmdS3Key = "outputs/face/" + cmdBasename;
                // Get the image from the cantaloupe image server
                String urlStr = "https://images.apollo-cttso.com/iiif/2/" + cmdS3Key.replace("/", "%2F").replace("+", "%2B") + "/full/full/0/default.jpg";
                Log.i("ViewResults urlStr: ", urlStr);
                new DisplayImageTask(imageBitmaps, cmdBasename, position).execute(urlStr);
            }
        }
    }

    private static class DisplayImageTask extends AsyncTask<String, Void, Bitmap> {
        List<Bitmap> bitmaps;
        String cmdBasename;
        Integer position;
        public DisplayImageTask(List<Bitmap> bitmaps, String cmdBasename, Integer position) {
            this.bitmaps = bitmaps;
            this.cmdBasename = cmdBasename;
            this.position = position;
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
                Log.i("ViewResults cmdPathStr", cmdBasename);
                FileUtils.saveBitmap(bitmap, cmdBasename);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        protected void onPostExecute(Bitmap bitmap) {
            bitmaps.set(position, bitmap);
        }
    }

}
