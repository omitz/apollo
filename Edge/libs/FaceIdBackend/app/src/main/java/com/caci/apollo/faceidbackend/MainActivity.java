package com.caci.apollo.faceidbackend;

//import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
// import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import com.caci.apollo.face_id_library.FaceRecognitionModel;

import java.io.IOException;
import java.util.List;


/**
* An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker
* to detect and then track objects.
*/
// public class MainActivity extends CameraActivity implements OnImageAvailableListener {
public class MainActivity extends CameraActivity {
// public class MainActivity extends AppCompatActivity {

    /*************************************
     * Constants 
     ************************************/
    private static String TAG = "Tommy MainActivity";
    private static final Size DESIRED_PREVIEW_SIZE = new Size (640, 480); 
    // private static final Size DESIRED_PREVIEW_SIZE = new Size (480, 480); 
    // private static final Size DESIRED_PREVIEW_SIZE = new Size (640, 360); 
    // private static final Size DESIRED_PREVIEW_SIZE = new Size (300, 300); 
    // private static final int PERMISSIONS_REQUEST = 13; // make it uniqe..


    /*************************************
     * member variables
     ************************************/
    private FaceRecognitionModel m_faceID;
    private Bitmap               m_rgbFrameBitmap = null;
    private byte[]               m_luminanceCopy = null;
    private boolean              m_computingDetection = false;
    OverlayView                  m_trackingOverlay;
    private int                  m_frameNo = 0;
    private boolean              m_initialized = false;
    // private String[]             m_permissions = new String[] {
    //     Manifest.permission.READ_EXTERNAL_STORAGE,
    //     Manifest.permission.WRITE_EXTERNAL_STORAGE,
    //     Manifest.permission.CAMERA,
    //     Manifest.permission.ACCESS_FINE_LOCATION,
    //     Manifest.permission.ACCESS_COARSE_LOCATION,
    //     Manifest.permission.INTERNET
    // };
    
    /*************************************
     * data structures
     ************************************/

    /*************************************
     * member functions
     ************************************/
    public static boolean hasPermission (Activity activity, String[] permissionsList) {
        Log.d ("Tommy", "calling hasPermission");
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState); // CameraActivity
        // setContentView(R.layout.activity_main);

        Log.d (TAG, "calling onCreate");

        // if (hasPermission (this, m_permissions)) {
        //     Log.d (TAG, "Has permssions already");
        //     init ();
        // } else {
        //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //         Log.d (TAG, "calling requestPermissions with " + PERMISSIONS_REQUEST);
        //         requestPermissions (m_permissions, PERMISSIONS_REQUEST);
        //     } else {
        //         Log.d (TAG, "ERROR: need to get permission..");
        //         assert (false);
        //     }
        // }
        // Log.d (TAG, "Done calling onCreate");

        
        // // Load the face models (embedding space and classifier)
        // m_faceID = new FaceRecognitionModel (true); // enable tracking
        // try {
        //     m_faceID.LoadModels (this);
        // } catch (Exception e) {
        //     e.printStackTrace();
        //     Log.d (TAG, "error Loading model");
        //     assert (false);
        //     return;
        // }

        // Log.d (TAG, "onCreate m_faceID = " + m_faceID);
        // // m_initialized = true;
        
        // Log.d (TAG, "done calling onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();

        if (m_initialized)
            return;
        
        // We already requested permissions in the parent onCreate
        Log.d (TAG, "calling onResume");
        if (hasPermission (this, base_permissions)) {
            Log.d (TAG, "has base permissions");
            init ();
        } 
        Log.d (TAG, "done calling onResume");
    }
    
    // @Override
    // public void onRequestPermissionsResult (int requestCode,
    //                                         String[] permissions,
    //                                         int[] grantResults) {
    //     Log.d (TAG, "calling onRequestPermissionsResult");
    //     super.onRequestPermissionsResult // CameraActivity requests permissions too
    //         (requestCode, permissions, grantResults);
    //     Log.d (TAG, "checking requestCode = " + requestCode);
    //     if (requestCode == PERMISSIONS_REQUEST) {
    //         if (grantResults.length > 0
    //             && grantResults[0] == PackageManager.PERMISSION_GRANTED
    //             && grantResults[1] == PackageManager.PERMISSION_GRANTED
    //             && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
    //             init ();
    //         } else {
    //             Log.d (TAG, "permission not met, " + requestCode + " " + grantResults.length);
    //             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    //                 Log.d (TAG, "calling requestPermissions again with " + PERMISSIONS_REQUEST);
    //                 requestPermissions (m_permissions, PERMISSIONS_REQUEST);
    //             }
    //         }
    //     }
    //     Log.d (TAG, "done calling onRequestPermissionsResult");
    // }

    private void init () {
        // Load the face models (embedding space and classifier)
        Log.d (TAG, "calling init");
        m_faceID = new FaceRecognitionModel (true); // enable tracking
        try {
            m_faceID.LoadModels (this);
            //m_faceID.LoadModels (this, "/sdcard/Download/facerecognizer_v2/", true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d (TAG, "error Loading model");
            m_initialized = false;
            return;
        }

        Log.d (TAG, "onCreate m_faceID = " + m_faceID);
        Log.d (TAG, "done calling onCreate");
        m_initialized = true;
    }

    
    @Override
    protected void processImage () {
        // Log.d (TAG, "calling processImage, m_faceID = " + m_faceID);
        // Log.d (TAG, "luminanceStride = " + getLuminanceStride());
        ++m_frameNo;
        // Log.d (TAG, "processImage()  m_frameNo = " + m_frameNo);
        

        // tracking at frame rate
        byte[] originalLuminance = getLuminance();
        m_faceID.TrackFaces (originalLuminance, m_frameNo);
        m_trackingOverlay.postInvalidate();        // request tracking overlay
        
        // No mutex needed as this method is not reentrant.
        // if (m_computingDetection || !m_initialized) {
        if (m_computingDetection || (m_rgbFrameBitmap == null)) {
            // Log.d (TAG, "too slow, skip frame " + m_frameNo);
            readyForNextImage();
            return;
        }
        m_computingDetection = true;

        // grab the image and release the source
        m_rgbFrameBitmap.setPixels // preview* varialbes are inherited from CameraActivity
            (getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
        if (m_luminanceCopy == null) {
            m_luminanceCopy = new byte[originalLuminance.length];
        }
        final long currFrameNo = m_frameNo;
        System.arraycopy (originalLuminance, 0, m_luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        
        // do longer proceessing in a thread
        // Log.d (TAG, "about to call runInBackground, m_faceID = " + m_faceID);
        runInBackground         // see CameraActivity.java
            (        
             () -> {
                 // m_trackingOverlay.postInvalidate(); // TC 2021-04-05 (Mon) -- TBD
                 
                 List<FaceRecognitionModel.FaceInfo> faceInfos;
                 faceInfos = m_faceID.PredictFaces (m_rgbFrameBitmap, currFrameNo, m_luminanceCopy);

                 if (faceInfos != null && faceInfos.size() > 0) {
                     for (FaceRecognitionModel.FaceInfo faceInfo : faceInfos) {
                         Log.d(TAG, "-----");
                         Log.d(TAG, "name = " + faceInfo.name);
                         Log.d(TAG, "confidence = " + faceInfo.confidence);
                         Log.d(TAG, "location = " + faceInfo.location);

                         // Get profile picture for the recognized face
                         try {
                             Bitmap profileBitmap = m_faceID.GetProfilePic (this,
                                                                            faceInfo.name);
                             String profileDebug =
                                 String.format ("/sdcard/Download/profile_%s.png", faceInfo.name);
                             m_faceID.SaveBitmap (profileBitmap, profileDebug);
                             Log.d (TAG, "saved profile pic to " + profileDebug);
                         } catch (IOException e) {
                             e.printStackTrace();
                             Log.d (TAG, "Error getting profile picture for " + faceInfo.name);
                         }
                     }
                 }

                 m_computingDetection = false;
                 // Log.d (TAG, "set m_computingDetection to false");
             });
    }

    @Override
    protected void onPreviewSizeChosen(Size size, int rotation) {
        Log.d (TAG, "calling onPreviewSizeChosen");
        m_rgbFrameBitmap = Bitmap.createBitmap (previewWidth, previewHeight,
                                                Bitmap.Config.ARGB_8888);
        // Log.d (TAG, "created m_rgbFrameBitmap, rotation = " + rotation +
        // update the sreen orientation.  (0 = portrait)

        // BUG: getScreenOrientation() always return 0??  
        Log.d (TAG, "sreen orientation = " + getScreenOrientation());
        m_faceID.SetOrientation (90); // assume screen is in portrait
                                      // mode, if not, use 0 or 180 for
                                      // landscape mode

        // overlay draws boxes on the image
        m_trackingOverlay = findViewById (R.id.tracking_overlay);
        m_trackingOverlay.addCallback
            (canvas -> {
                // Log.d (TAG, "calling trackingOverlay callback");
                // tracker.draw (canvas);
                // if (m_initialized)
                if (m_rgbFrameBitmap != null)
                    m_faceID.DrawTrackingOverlay (canvas);
            });

        // Log.d (TAG, "Set m_initialized = True");
        // m_initialized = true;
    }

    @Override
    protected int getLayoutId() {
        Log.d (TAG, "calling getLayoutId");
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        Log.d (TAG, "calling getDesiredPreviewFrameSize");
        Log.d (TAG, DESIRED_PREVIEW_SIZE.getWidth() + " " + DESIRED_PREVIEW_SIZE.getHeight());
        return DESIRED_PREVIEW_SIZE;
    }


    /*************************************
     * helper functions
     ************************************/
    
}
