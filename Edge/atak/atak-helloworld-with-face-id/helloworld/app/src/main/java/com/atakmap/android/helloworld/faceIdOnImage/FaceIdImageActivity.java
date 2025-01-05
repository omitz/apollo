package com.atakmap.android.helloworld.faceIdOnImage;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.atakmap.android.helloworld.plugin.R;
import com.caci.apollo.face_id_library.FaceRecognitionModel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.os.SystemClock.sleep;

public class FaceIdImageActivity extends Activity {

    /*************************************
     * Constants 
     ************************************/
    private static String TAG = "Tommy FaceIdImage";
    private static final int CAMERA_REQUEST = 8887;
    private static final String CAMERA_INFO = "com.atakmap.android.helloworld.FACEID";
    private static final int PERMISSIONS_REQUEST = 1;

    
    /*************************************
     * member variables
     ************************************/
    private FaceRecognitionModel m_faceID = null;
    private ImageView m_imgPreview;
    private ImageView m_profilePic;
    private String m_photoPath;

    /*************************************
     * member variables
     ************************************/
    private String[] m_permissions = new String[] {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    };

    /*************************************
     * member functions
     ************************************/
    public static boolean hasPermission (Activity activity, String[] permissionsList) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String permission : permissionsList) {
                boolean granted = activity.checkSelfPermission(permission) ==
                    PackageManager.PERMISSION_GRANTED;
                if (!granted) {
                    allGranted = false;
                }
            }
            return allGranted;
        } else {
            return true;
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_id);
        m_imgPreview = findViewById (R.id.faceID_imgPreview);
        m_profilePic = findViewById (R.id.faceID_profilePic);

        if (hasPermission (this, m_permissions)) {
            init ();
        } else {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions (m_permissions, PERMISSIONS_REQUEST);
             } else {
                 Log.d (TAG, "ERROR: need to get permission..");
                 assert (false); // TBF
             }
        }
        Log.d (TAG, "Done calling onCreate");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        Log.d (TAG, "calling onRequestPermissionsResult");
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                init ();
            } else {
                Log.d (TAG, "permission not met, " + requestCode + " " + grantResults.length);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions (m_permissions, PERMISSIONS_REQUEST);
                }
            }
        }
        Log.d (TAG, "done calling onRequestPermissionsResult");
    }

    private boolean init () {
        Log.d (TAG, "Calling init()");

        // 1.) Create the backend
        m_faceID = new FaceRecognitionModel (false); // disable tracking if id a single image

        // 2.) Load the models
        try {
            m_faceID.LoadModels (this);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d (TAG, "Error loading model");
            return false;
        }

        // 3.) capture an image
        Log.d (TAG, "calling IMAGE_CAPTURE");
        dispatchTakePictureIntent ();
        return true;
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                                         imageFileName,  /* prefix */
                                         ".jpg",         /* suffix */
                                         storageDir      /* directory */
                                         );
        
        // Save a file: path for use with ACTION_VIEW intents
        m_photoPath = image.getAbsolutePath();
        return image;
    }

    
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d (TAG, "Error occurred while creating the File");
                return;
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile
                    (this,
                     "com.atakmap.android.helloworld.plugin",
                     photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult (takePictureIntent, CAMERA_REQUEST);
            }
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult (requestCode, resultCode, data);

        if (requestCode != CAMERA_REQUEST || resultCode != Activity.RESULT_OK) {
            Log.d (TAG, "skip onActivityResult");
            return;
        }
            
        Bitmap photo = null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true; // bitmap must be mutable for OverlayFaceIDs()
            Bitmap photo_ori = BitmapFactory.decodeFile (m_photoPath, options);

            // Delete m_photoPath -- we don't need it anymore and to save disk space.
            File delme = new File (m_photoPath);
            Log.d (TAG, "deleting " + m_photoPath);
            delme.delete ();

            // Scale the image down to about 640x480 so the overlay font is just right
            float width_ori = photo_ori.getWidth();
            float height_ori = photo_ori.getHeight();
            int height = 480;   // 
            int width = (int) ((width_ori / height_ori) * height);
            photo = Bitmap.createScaledBitmap (photo_ori, width, height, true);

            // Rotate the image so that it is up-side up.  (This
            // assumes the screen is in Portrait mode)
            Matrix matrix = new Matrix();
            matrix.postRotate (90); // For some phones, use matrix.postRotate(0) instead.
            photo = Bitmap.createBitmap (photo, 0, 0, width, height, matrix, true);
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.d (TAG, "could not load capture image");
            return;
        }


        // Now that the image is upside up, there is no need to rotate the image.
        m_faceID.SetOrientation (0); 

        
        // Id the face here:
        // Compute face embeddings and predict the faces
        List<FaceRecognitionModel.FaceInfo> faceInfos;
        faceInfos = m_faceID.PredictFaces (photo);
        if (faceInfos != null && faceInfos.size() > 0) {

            // only find the highest prob
            int maxIdx = 0;
            float maxConf = 0;
            for (int idx = 0; idx < faceInfos.size(); idx++) {
                Log.d(TAG, "-----");
                Log.d(TAG, "name = " + faceInfos.get(idx).name);
                Log.d(TAG, "confidence = " + faceInfos.get(idx).confidence);
                if (faceInfos.get(idx).confidence > maxConf) {                    
                    maxConf = faceInfos.get(idx).confidence;
                    maxIdx = idx;
                }
            }
            FaceRecognitionModel.FaceInfo bestFaceInfo = faceInfos.get(maxIdx);

            
            // Get profile picture for the recognized face
            try {
                Bitmap profileBitmap = m_faceID.GetProfilePic (this, bestFaceInfo.name);
                m_profilePic.setImageBitmap (profileBitmap);
            } catch (IOException e) {
                e.printStackTrace();
                Log.d (TAG, "Error getting profile picture for " + bestFaceInfo.name);
            }
                
            
            //  Overlay the detection box on the image
            assert (photo.isMutable()); // overlay requires image be mutable
            faceInfos.clear();
            faceInfos.add (bestFaceInfo);
            m_faceID.OverlayFaceIDs (photo, faceInfos, 24.0f, Color.RED);
            
        } else {
            Log.d (TAG, "No face detected = ");
        }
        // show the image, regardless the faceID result
        m_imgPreview.setImageBitmap (photo);
        // m_faceID.SaveBitmap (photo, "/sdcard/Download/faceIdResult.png"); // debugging..
    }
}
