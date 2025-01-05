package com.atakmap.android.apolloedge.pp.facerecognizer.utils;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import com.atakmap.android.apolloedge.pp.facerecognizer.Classifier;
import com.atakmap.android.apolloedge.pp.facerecognizer.utils.ExifTags;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static com.atakmap.android.apolloedge.pp.facerecognizer.utils.GeolocUtils.convertDegreesToDMS;

public class FileUtils {
    public static final String ROOT =
            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "facerecognizer";

    public static final String DATA_FILE = "data";
    public static final String MODEL_FILE = "model";
    public static final String LABEL_FILE = "label";
    public static final String ASSETS_SUBDIR = "face";
    public static final String VIPS_SUBDIR = "vips";
    public static final String VIPS_ASSET_PATH = ASSETS_SUBDIR + File.separator + VIPS_SUBDIR;
    public static final String DATA_PATH = FileUtils.ROOT + File.separator + DATA_FILE;
    public static final String MODEL_PATH = FileUtils.ROOT + File.separator + MODEL_FILE;
    public static final String REF_JSON = FileUtils.ROOT + File.separator + "ref.json";
    public static final String MOBILE = "mobile";
    public static final String COMMAND = "command";
    public static final String IMAGE_PATH_EXTRA_NAME = "IMAGE_PATH";
    public static final String ORIGINAL_IMAGE_PATH_BUNDLE_KEY = "originalImagePath";
    public static final String IMAGE_PATH_BUNDLE_KEY = "imagePath";
    public static final String POSITION_BUNDLE_KEY = "position";


    /**
     * Saves a Bitmap object to disk.
     *  @param bitmap The bitmap to save.
     * @param filename The location to save the bitmap to.
     * @param location null OR Location object (contains lat/long in degrees; eg provided by UtilsLocation requestLoc).
     */
    public static String saveBitmap(final Bitmap bitmap, final String filename, Location location) {
        String filepath = ROOT + File.separator + filename;

        final File file = new File(filepath);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 99, out); // ExifInterface supports JPEG
            out.flush();
            out.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String latRef;
            String longRef;
            // Get refs (ie cardinal point)
            if (latitude < 0) {
                latRef = "S";
            } else {
                latRef = "N";
            }
            if (longitude < 0) {
                longRef = "W";
            } else {
                longRef = "E";
            }
            // Convert degrees to DMS
            String latStr = convertDegreesToDMS(latitude);
            String longStr = convertDegreesToDMS(longitude);
            // Write geoloc data to file
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(filepath);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latStr);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, longStr);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longRef);
                exif.saveAttributes();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filepath;
    }
    /**
     * Saves a Bitmap object to disk.
     *  @param bitmap The bitmap to save.
     * @param filename The location to save the bitmap to.
     * @param exifTags null OR ExifTags object.
     */
    public static String saveBitmap(final Bitmap bitmap, final String filename, ExifTags exifTags) {
        String filepath = ROOT + File.separator + filename;

        final File file = new File(filepath);
        if (file.exists()) {
            file.delete();
        }
        try {
            final FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 99, out); // ExifInterface supports JPEG
            out.flush();
            out.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        if (exifTags != null) {
            // Write geoloc data to file
            ExifInterface exif = null;
            try {
                exif = new ExifInterface(filepath);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, exifTags.TAG_GPS_LATITUDE);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, exifTags.TAG_GPS_LONGITUDE);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, exifTags.TAG_GPS_LATITUDE_REF);
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, exifTags.TAG_GPS_LONGITUDE_REF);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return filepath;
    }

    public static void appendText(String text, String filename) {
        try(FileWriter fw = new FileWriter(ROOT + File.separator + filename, true);
            PrintWriter out = new PrintWriter(new BufferedWriter(fw))) {
            out.println(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<String> readLabel(String filename) throws FileNotFoundException{
        File file = new File(ROOT + File.separator + filename);
        Scanner s = new Scanner(file);
        ArrayList<String> list = new ArrayList<>();
        while (s.hasNextLine()){
            list.add(s.nextLine());
        }
        s.close();

        return list;
    }

    public static Bitmap getBitmapFromUri(ContentResolver contentResolver, Uri uri) throws Exception {
        ParcelFileDescriptor parcelFileDescriptor =
                contentResolver.openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return bitmap;
    }

    public static String saveResultsToJson(List<Classifier.Recognition> mappedRecognitions, String uid, String fName) {
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
        String path = ROOT + File.separator + fNameResults;
        final File file = new File(path);
        String stringResults = resultsJson.toString();

        writeStringToFile(file, stringResults);
        return fNameResults;
    }

    public static void writeStringToFile(File file, String stringResults) {
        try {
            Writer output;
            output = new BufferedWriter(new FileWriter(file));
            output.write(stringResults);
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    public static JSONObject readJsonObjectFromFile(File file) throws IOException, JSONException {
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
        return new JSONObject(response);
    }

    public static void saveMobileResultToRefJson(String originalFn, String mobileFn, String jsonFn) {
        // create or load the reference json (a json file containing all processed images and the filepaths for their results)
        File refJson = new File(REF_JSON);
        JSONObject refJsonObj = new JSONObject();
        if (!refJson.exists()) {
            refJsonObj = new JSONObject();
        } else {
            try {
                refJsonObj = readJsonObjectFromFile(refJson);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
        JSONObject contents = new JSONObject();
        try {
            contents.put(MOBILE, mobileFn);
            contents.put("json", jsonFn);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            refJsonObj.put(originalFn, contents.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        writeStringToFile(refJson, refJsonObj.toString());
    }

    public static void saveCommandResultToRefJson(String originalFn, String cmdFn) throws JSONException {
        // Load the reference json (a json file containing all processed images and the filepaths for their results)
        File refJson = new File(REF_JSON);
        JSONObject refJsonObj = new JSONObject();
        try {
            refJsonObj = readJsonObjectFromFile(refJson);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        JSONObject contents = new JSONObject((String) refJsonObj.get(originalFn));
        try {
            contents.put(COMMAND, cmdFn);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        refJsonObj.put(originalFn, contents);
        writeStringToFile(refJson, refJsonObj.toString());
    }
}
