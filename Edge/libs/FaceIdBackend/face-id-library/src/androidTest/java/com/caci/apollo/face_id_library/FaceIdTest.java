package com.caci.apollo.face_id_library;

import android.Manifest;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class FaceIdTest {
    private static String TAG = "Tommy FaceIdTest";


    @Rule
    public GrantPermissionRule permissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE);

    
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.caci.apollo.face_id_library.test", appContext.getPackageName());
    }


    @Test
    public void test1() {

        String param = BuildConfig.TEST_PARAM1;
        Log.d (TAG, "calling test1 with param = " + param);
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        /*
         * 1.) Create the backend
         */
        FaceRecognitionModel faceID = new FaceRecognitionModel (false); // enable tracking or not

        
        /*
         * 2.) Load the models
         */
        try {
            if (param.equals ("assets")) {
                // To load default model included with the library:
                faceID.LoadModels (appContext);
            } else if (param.equals ("data_package")) {
                // For loading models from ATAK data package location
                // (data package must be imported already)
                // faceID.LoadModels (appContext, "faceID_celebrity10");
                faceID.LoadModels (appContext, "ApolloFaceID-10VIP");
            } else {
                Log.d (TAG, "invalid param = " + param);
                assertTrue(false);
                return;
            }
            // To load model from elsewhere:
            // faceID.LoadModels (appContext, "/sdcard/Download/facerecognizer_v2/", true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d (TAG, "Error loading model");
            return;
        }


        /*
         * 3.) Get a query image (from the assets folder) and set the
         * correct orientation.
         */
        AssetManager assetManager = appContext.getAssets();
        InputStream query = null;
        Bitmap imgData = null;
        // String imgFile = "face_demo_img.jpg";
        String imgFile = "jim_gaffigan.jpg";
        try {
            query = assetManager.open (imgFile);
            Log.d (TAG, "qeury size available = " + query.available());
            imgData = faceID.DecodeImageData (query);
            query.close (); 
        } catch (IOException e) {
            e.printStackTrace();
            Log.d (TAG, "Error opening query image file");
            return;
        }
        faceID.SetOrientation (0); // this image is upside-up, (ie.,
                                   // taken with landscape
                                   // orientation)

        
        /*
         * 4.) Compute face embeddings and predict the faces
         */
        List<FaceRecognitionModel.FaceInfo> faceInfos;
        faceInfos = faceID.PredictFaces (imgData);
        if (faceInfos != null && faceInfos.size() > 0) {
            for (FaceRecognitionModel.FaceInfo faceInfo : faceInfos) {
                Log.d(TAG, "-----");
                Log.d(TAG, "name = " + faceInfo.name);
                Log.d(TAG, "confidence = " + faceInfo.confidence);
                Log.d(TAG, "location = " + faceInfo.location);

                // Get profile picture for the recognized face
                try {
                    Bitmap profileBitmap = faceID.GetProfilePic (appContext, faceInfo.name);
                    String profileDebug = String.format ("/sdcard/Download/profile_%s.png",
                                                         faceInfo.name);
                    faceID.SaveBitmap (profileBitmap, profileDebug);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.d (TAG, "Error getting profile picture for " + faceInfo.name);
                    assertTrue (false);
                    return;
                }
            };
        } else {
            Log.d (TAG, "No face detected = ");
        }

        
        /*
         * 5.) Overlay the detection box onto the image and save it
         */
        Bitmap overlayBitmap = imgData.copy (Bitmap.Config.ARGB_8888, true);
        assertTrue (overlayBitmap.isMutable());
        faceID.OverlayFaceIDs (overlayBitmap, faceInfos, 32.0f, Color.RED);
        faceID.SaveBitmap (overlayBitmap,  "/sdcard/Download/output.png");

        Log.d (TAG, "done test1.");
    }
}

