package ocr;

import android.Manifest;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import apollo_utils.ApolloFileUtils;
import apollo_utils.PermissionsUtils;
import pp.facerecognizer.R;
import pp.facerecognizer.utils.FileUtils;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST = 1;
    final int REQUEST_IMAGE_CAPTURE = 2;
    final int REQUEST_IMAGE_IMPORT = 3;
    public static final String TESSDATA_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tessdata";
    private static final String OCR_ASSETS = "ocr";
    private static final String[] DATA_FILES = {"eng.tessdata", "ara.traineddata", "eng.traineddata", "fra.traineddata", "rus.traineddata"};
    private static final String EXAMPLE_TEXT_DIR = "ocr_example_text";
    private String[] permissions = new String[] {
            Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public File imagePath;
    public Uri contentUri;
    public String timeStamp;
    public File storageDir;
    String result;
    // Create a handler view on the main thread (thus it will only work with the message queue of the main thread)
    private Handler mainHandler = new Handler();
    ProgressBar pgsBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ocr_activity_main);
        requestPermissions(permissions, PERMISSIONS_REQUEST);

        if (PermissionsUtils.hasPermission(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE})) {
            // Copy tessdata to the device
            File dir = new File(TESSDATA_DIR);

            if (!dir.isDirectory()) {
                dir.mkdirs();
            }

            // Copy the language model files onto the phone
            AssetManager mgr = getAssets();
            for (String dataFile : DATA_FILES) {
                String srcFilename = OCR_ASSETS + File.separator + dataFile;
                String destFilepath = TESSDATA_DIR + File.separator + dataFile;
                ApolloFileUtils.copyAsset(mgr, srcFilename, destFilepath);
            }
            // Copy the demo images onto the phone
            String destDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + EXAMPLE_TEXT_DIR;
            File examplesDir = new File(destDirPath);
            examplesDir.mkdirs();
            String srcDir = OCR_ASSETS + File.separator + EXAMPLE_TEXT_DIR;
            String[] imgFiles = null;
            try {
                imgFiles = mgr.list(srcDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (imgFiles != null) {
                for (String imgFile : imgFiles) {
                    String srcFilename = srcDir + File.separator + imgFile;
                    String destFilename = examplesDir + File.separator + imgFile;
                    ApolloFileUtils.copyAsset(mgr, srcFilename, destFilename);
                }
            }
        }

        setDefaultLang();

        Button btOpenCam = findViewById(R.id.openCamBt);
        btOpenCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCam();
            }
        });

        Button importBtn = findViewById(R.id.importButton);
        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ApolloFileUtils.performFileSearch(MainActivity.this, 3);
            }
        });

        Button setLangBtn = findViewById(R.id.setLangButton);
        setLangBtn.setOnClickListener(v ->
        {
            Intent langActivity = new Intent(getBaseContext(), LanguageActivity.class);
            startActivity(langActivity);
        });

        pgsBar = findViewById(R.id.indeterminateBar);
        // Hide the progress circle for now
        pgsBar.setVisibility(View.GONE);

        // Check if we are receiving a file from SCANNER!
        Bundle extras = getIntent().getExtras();
        Bitmap bitmap = null;
        String uriString;
        if (extras != null && extras.getBoolean("isScanned")) {
            uriString = extras.getString("uri");
            Uri imageUri = Uri.parse(uriString);
            try {
                bitmap = FileUtils.getBitmapFromUri(this.getContentResolver(), imageUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (bitmap != null) {
                ImageView imgView = (ImageView) findViewById(R.id.imageView);
                imgView.setImageBitmap(bitmap);
                OcrThread ocrThread = new OcrThread(bitmap, UtilsOCR.curLang);
                ocrThread.start();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        TextView curLangView = findViewById(R.id.curLangView);
        Map<String, String> langs = UtilsOCR.getLangs();
        curLangView.setText("  Current language: " + langs.get(UtilsOCR.curLang));
    }

    private void createImageFile() throws IOException {
        // Create an image file name
        timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        imagePath = new File(image.getAbsolutePath());
    }

    private void openCam() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // resolveActivity returns the first activity component that can handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            try {
                createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // if the file was successfully created
            if (imagePath != null) {
                contentUri = FileProvider.getUriForFile(this, "pp.facerecognizer.fileprovider", imagePath);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void setDefaultLang() {
        // Default to english in case the user didn't select a language
        if (UtilsOCR.curLang == null) {
            UtilsOCR.curLang = "eng";
            UtilsOCR.tessOCR = new TessOCR(UtilsOCR.curLang);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            // Clear out any previous results
            display("");
            pgsBar.setVisibility(View.VISIBLE); //to show
            Bitmap bitmap = null;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                String pathStr = String.valueOf(imagePath);
                // Get the fullsize image we saved
                Bitmap possiblyRotated = BitmapFactory.decodeFile(pathStr);

                // Check that it's orientated correctly and rotate if not
                // Warning: This USUALLY works, but not always because sometimes the exif data is wrong. (Seems like a known bug.) The solution for now is to display the bitmap exactly as it will be passed to TessOCR, so that the user will be able to see that the image needs to be rotated. Then onus is then on the user to rotate the image in the app of their choosing and run OCR using IMPORT IMAGE.
// See https://nextcentury.atlassian.net/wiki/spaces/AP/pages/624066615/Document+and+Text+Processing#DocumentandTextProcessing-Currentappstatus: for visual examples of success and (TODO) fail cases
                ExifInterface exif = null;
                try {
                    exif = new ExifInterface(pathStr);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED);

                // TODO Use this toast message to help debug the rotation issue
//                Toast.makeText(getApplicationContext(), Integer.toString(orientation), Toast.LENGTH_LONG).show();

                Bitmap possiblyDark = rotateBitmap(possiblyRotated, orientation);
                // Note: The contrast and brightness values specified here are eyeball estimates. Ideally, we'd set these value programmatically based on some characteristic of the photo.
                bitmap = changeBitmapContrastBrightness(possiblyDark, 2, 4);
            } else if (requestCode == REQUEST_IMAGE_IMPORT) {
                // The result data contains a URI for the image that the user selected.
                Uri imageUri = data.getData();
                // Perform operations on the document using its URI
                try {
                    bitmap = FileUtils.getBitmapFromUri(this.getContentResolver(), imageUri);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (bitmap != null) {
                ImageView imgView = (ImageView) findViewById(R.id.imageView);
                imgView.setImageBitmap(bitmap);
                OcrThread ocrThread = new OcrThread(bitmap, UtilsOCR.curLang);
                ocrThread.start();
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * https://stackoverflow.com/questions/12891520/how-to-programmatically-change-contrast-of-a-bitmap-in-android
     * @param bmp input bitmap
     * @param contrast 0..10 1 is default
     * @param brightness -255..255 0 is default
     * @return new bitmap
     */
    public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness) {
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    private void display(String result) {
        TextView resultTextView = findViewById(R.id.resultTextView);
        resultTextView.setText(result);
    }

    class OcrThread extends Thread {
        Bitmap bitmap;
        String lang;

        public OcrThread(Bitmap bitmap, String lang) {
            this.bitmap = bitmap;
            this.lang = lang;
        }

        @Override
        public void run() {

            result = UtilsOCR.tessOCR.getOCRResult(bitmap);

            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    pgsBar.setVisibility(View.GONE);
                    display(result);
                }
            });

            JSONObject resultJson = new JSONObject();
            try {
                resultJson.put("result:", result);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            final String jsonName = String.format("%s.json", timeStamp);
            final File file = new File(storageDir, jsonName);

            try {
                Writer output;
                output = new BufferedWriter(new FileWriter(file));
                output.write(resultJson.toString());
                output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
