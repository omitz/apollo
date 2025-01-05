package com.atakmap.android.apolloedge.face_recognition;

import com.atakmap.android.apolloedge.plugin.R;
import com.caci.apollo.face_id_library.FaceRecognitionModel;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ImageReader.OnImageAvailableListener;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import java.io.IOException;
import java.util.List;

/**
 * Entry point for face recognition; An activity that uses a
 * TensorFlowMultiBoxDetector and ObjectTracker to detect and then
 * track objects.
 */
public class FaceRecognitionActivity extends CameraActivity implements OnImageAvailableListener {

    /*************************************
     * Constants 
     ************************************/
    String TAG = "Tommy FaceRecognitionActivity";
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);


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


    /*************************************
     * data structures
     ************************************/


    /*************************************
     * inheritance 
     ************************************/
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d (TAG, "calling onCreate");

        /* Tie GUI callbacks */
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if (m_initialized)
            return;
        
        // We already requested permissions in the parent onCreate
        Log.d (TAG, "calling onResume");
        if (hasPermission ((Activity) this, base_permissions)) {
            Log.d (TAG, "has base permissions");
            init ();
        } 
        Log.d (TAG, "done calling onResume");
    }

    @Override
    public void finish() {
        super.finishAndRemoveTask();
    }

    @Override
    protected void processImage() {
        ++m_frameNo;
        
        // tracking at frame rate
        byte[] originalLuminance = getLuminance();
        m_faceID.TrackFaces (originalLuminance, m_frameNo);
        m_trackingOverlay.postInvalidate();        // request tracking overlay
        
        // No mutex needed as this method is not reentrant.
        if (m_computingDetection || (m_rgbFrameBitmap == null)) {
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
                             Log.d (TAG, "got profile pic " + faceInfo.name);
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
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        Log.d (TAG, "calling onPreviewSizeChosen");
        Log.d (TAG, "previewWidth = " + previewWidth);
        Log.d (TAG, "previewHeight = " + previewHeight);
        m_rgbFrameBitmap = Bitmap.createBitmap (previewWidth, previewHeight,
                                                Bitmap.Config.ARGB_8888);
        Log.d (TAG, "created m_rgbFrameBitmap = ");

        // BUG: getScreenOrientation() always return 0??  
        Log.d (TAG, "sreen orientation = " + getScreenOrientation());
        m_faceID.SetOrientation (90); // assume screen is in portrait
                                      // mode, if not, use 0 or 180 for
                                      // landscape mode
        
        // overlay draws boxes on the image
        m_trackingOverlay = findViewById (R.id.tracking_overlay_v2);
        m_trackingOverlay.addCallback
            (canvas -> {
                if (m_rgbFrameBitmap != null)
                    m_faceID.DrawTrackingOverlay (canvas);
            });
    }    


     @Override
     protected int getLayoutId() {
         return R.layout.camera_connection_fragment_tracking_v2;
     }


    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }
    
    
    /*************************************
     * helper functions
     ************************************/
    private void init () {
        // Load the face models (embedding space and classifier)
        Log.d (TAG, "calling init");
        m_faceID = new FaceRecognitionModel (true); // enable tracking
        try {
            // m_faceID.LoadModels (this);
            m_faceID.LoadModels (this, "ApolloFaceID-10VIP"); // use atak data package
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
    
}

