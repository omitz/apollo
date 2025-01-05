package com.caci.apollo.face_id_library;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import com.caci.apollo.face_id_library.env.BorderedText;
import com.caci.apollo.face_id_library.env.FaceIdFileUtils;
import com.caci.apollo.face_id_library.env.ImageUtils;
import com.caci.apollo.face_id_library.tracking.MultiBoxTracker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Face Recognition Backend.
 * API usage example: See test1() of <a href="file:../../../../FaceIdTest.html">FaceIdTest.java</a>
 */
public class FaceRecognitionModel {

    /*************************************
     * Constants 
     ************************************/
    private static final int FACE_SIZE = 160;
    private static final int CROP_SIZE = 300; // for MTCNN, larger maybe more accurate recognition
    private static final float MIN_SIZE = 16.0f; // detection minium size
    private static final String ATAK_DATA_PACKAGE_DIR = "/sdcard/atak/tools/datapackage/files/"; // TBF...
    private static String TAG = "Tommy FaceRecogModel"; // proguard crash if delcare final...


    /*************************************
     * member variables
     ************************************/
    // private Bitmap rgbFrameBitmap = null;
    // private Bitmap croppedBitmap = null;

    private Matrix m_frameToCropTransform;
    private Matrix m_cropToFrameTransform;

    private boolean m_initialized = false;
    private Classifier classifier;

    private Bitmap m_croppedBitmap = null;
    // private Bitmap m_cropCopyBitmap = null;

    private int m_curPreviewWidth = 0;
    private int m_curPreviewHeight = 0;
    private Integer m_sensorOrientation = 90; // 0, 90 (portrait), 180, or 270

    // private ArrayList<FaceInfo> m_faceInfos = new ArrayList<FaceInfo>(10);
    private boolean m_useTracking= false;
    private boolean m_computingDetection = false;
    
    
    // Tracker stuff
    private MultiBoxTracker m_tracker = null;
//    private byte[] m_luminanceCopy;
    // private long m_timestamp = 0;
    

    /*************************************
     * data structures
     ************************************/
    public static class FaceInfo {
        /**
         * Display name for the recognition.
         */
        public String name;

        /**
         * A sortable score for how good the recognition is relative
         * to others. Higher should be better.
         */
        public Float confidence;

        /** location within the source image */
        public RectF location;
    }
    

    /*************************************
     * member functions
     ************************************/

    /**
     * Constructor
     * @param enableTracking Whether to enable tracking or not.
     * Tracking is needed for continuous video input (eg., live
     * camera).  Disable tracking if you are just processing a single static image.
     */
    public FaceRecognitionModel (boolean enableTracking) {
        m_croppedBitmap = Bitmap.createBitmap (CROP_SIZE, CROP_SIZE, Bitmap.Config.ARGB_8888);
        m_useTracking = enableTracking;
    }

    /**
     * Load pre-trained face models
     * @param ctx The android activity context (eg., type
     * AppCompatActivity).  Context is needed for copying embedding
     * model from the assets folder, if not already copied.  
     * @throws Exception model could not be loaded
     */
    public void LoadModels (Context ctx) throws Exception {
        LoadModels (ctx, "facerecognizer", false);
    }

    /**
     * Preload all model files
     * @param ctx The android activity context (eg., type
     * AppCompatActivity).  Context is needed for copying embedding
     * model from the assets folder, if not already copied.  
     * @param atakDataPackageUUID The uuid of the data package.  The
     * location of the data package is assumed to be located at
     * /sdcard/atak/tools/datapackage/files/<dataPackage_uuid>/ (eg.,
     * "ApolloFaceID-10VIP").  Note: data package is also known as
     * mission package.  Do not confuse it with "mission data feed",
     * which is a publish/subscribe synchronization service from the
     * TAK Server.
     * @throws IOException model could not be loaded
     */
    public void LoadModels (Context ctx, String atakDataPackageUUID) throws Exception {
        LoadModels (ctx, ATAK_DATA_PACKAGE_DIR + "/" + atakDataPackageUUID, true);
    }

    /**
     * Load pre-trained face models
     * @param ctx The android activity context (eg., type
     * AppCompatActivity).  Context is needed for copying embedding
     * model from the assets folder, if not already copied.  
     * @param rootDir A relative directory where the models are
     * located, default "facerecognizer".  If the directory does not
     * exists, it will be created and the default model that comes
     * with the library will be copied there.
     * @param useAbsolutePath set to true if rootDir is absolute path.
     * Otherwise relative to the application internal directory
     * (eg.,"/internal/storage/Android/data/com.your_app_name/files/")
     * @throws Exception model could not be loaded
     */
    public void LoadModels (Context ctx,
                            String rootDir,
                            boolean useAbsolutePath) throws Exception {
        
        Log.d (TAG, "Calling LoadModels");
        FaceIdFileUtils.setRoot (ctx, rootDir, useAbsolutePath);

        // For relative path, we need to copy from assets
        if (useAbsolutePath == false) {
            // If rootDir does not exists, we use the default model,
            // otherwise, we use whatever model that is in rootDir
            File dir = new File (FaceIdFileUtils.getRoot());
            if (!dir.exists()) {
                dir.mkdirs();
                AssetManager mgr = ctx.getAssets();
                Log.d (TAG, "copying default models");
                if ((!FaceIdFileUtils.copyAsset (mgr, FaceIdFileUtils.MODEL_FILE)) ||
                    (!FaceIdFileUtils.copyAsset (mgr, FaceIdFileUtils.PROFILE_FILE)) ||
                    (!FaceIdFileUtils.copyAsset (mgr, FaceIdFileUtils.LABEL_FILE))) {
                    Log.d (TAG, "Could not copy default SVM model!");
                    throw new Exception();
                }
            }
        }
        
        try {
            Log.d (TAG, "loading model from " + FaceIdFileUtils.getModelPath());
            classifier = Classifier.getInstance
                (ctx.getAssets(), FACE_SIZE, FACE_SIZE,
                 FaceIdFileUtils.getModelPath(),
                 FaceIdFileUtils.getDataPath());
        } catch (Exception e) {
            e.printStackTrace();                
            Log.d (TAG,"Exception initializing classifier!");
            throw new Exception();
        }
        

        // Create a tracker if needed
        if (m_useTracking)
            m_tracker = new MultiBoxTracker (ctx);
    }


    /**
     * Set the image/camera orientation.  
     * @param sensorOrientation Must be one of: 0, 90, 180, 270,
     * (Default = 90 for portrait mode.) 
     * @see "https://developer.android.com/reference/android/view/Display#getRotation()"
     * @return True if success
     */
    public boolean SetOrientation (int sensorOrientation) {
        if ((sensorOrientation == 0) ||
            (sensorOrientation == 90) ||
            (sensorOrientation == 180) ||
            (sensorOrientation == 270)) {
            m_sensorOrientation = sensorOrientation;
            return true;
        } else {
            Log.d (TAG, "sensorOrientation Must be one of: 0, 90, 180, 270");
            return false;
        }
    }

    
    /**
     * Decode image file
     * @param imageFileStream Content of image file to be decoded (eg., new FileInputStream(new File(ImageFileName))
     * @return The image data
     */
    public Bitmap DecodeImageData (final InputStream imageFileStream) {
        if (imageFileStream == null)
            return null;

        return BitmapFactory.decodeStream (imageFileStream);
    }

    /**
     * Save image to file
     * @param bitmap The image data to save
     * @param absFilename The absolute path of the image file.  
     */
    public void SaveBitmap (final Bitmap bitmap, final String absFilename) {
        FaceIdFileUtils.saveBitmap (bitmap, absFilename);
    }


    /**
     * Detect, recognize, and track faces from the previous frame
     * This function should be run in another thread.
     * @param rgbFrameBitmap image data
     * @param currTimestamp the current frame number. Ignored if tracking is disabled.
     * @param luminance luminance of rgbFrameBitmap. Ignored if tracking is disabled.
     * @return A list of faces and their meta info (see FaceInfo).  If
     * no face is detected, return either null or empty list.
     */
    public List<FaceInfo> PredictFaces (final Bitmap rgbFrameBitmap,
                                        final long currTimestamp,
                                        final byte[] luminance) {
        // Log.d (TAG, "calling PredictFaces at frame = " + currTimestamp);
        
        /*
         * 1.) Set the necessary transformation between input image and scaled image
         */
        if ((m_curPreviewWidth != rgbFrameBitmap.getWidth()) ||
            (m_curPreviewHeight != rgbFrameBitmap.getHeight())) {
            SetTransforms (rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight());
            m_initialized = true;
        }

        
        // No mutex needed as this method is not reentrant.
        if (m_computingDetection) {
            
            return null;
        }

        m_computingDetection = true;
        ArrayList<FaceInfo> faceInfos = new ArrayList<FaceInfo>(30); // less than 30 detections TBF

                
        /*
         * 2.) Crop image to a smaller size (TBF keep the aspect ratio?)
         */
        final Canvas canvas = new Canvas (m_croppedBitmap); // modifies m_croppedBitmap
        canvas.drawBitmap (rgbFrameBitmap, m_frameToCropTransform, null);
        // m_cropCopyBitmap = Bitmap.createBitmap (m_croppedBitmap);
        
        /*
         * 3.) detect and classify the face
         */
        List<Classifier.Recognition> mappedRecognitions = // result rectangles are in original image
            classifier.recognizeImage (m_croppedBitmap, m_cropToFrameTransform);
        // Log.d (TAG, "detections = " + mappedRecognitions.size());


        // add the new detection and add it to tracking:
        if (m_useTracking) {
            // Log.d (TAG, "Calling Track the results");
            m_tracker.trackResults (mappedRecognitions, luminance, currTimestamp);
        }

        /*
         * 4.) Pack the result
         */
        for (final Classifier.Recognition result : mappedRecognitions) {
            if (result.getLocation() == null) {
                continue;
            }
            // Log.d (TAG, "Result! Frame: " + result.getLocation());
            
            final RectF detectionFrameRect = new RectF (result.getLocation());
            // final RectF detectionScreenRect = new RectF();
            // rgbFrameToScreen.mapRect (detectionScreenRect, detectionFrameRect);  // TBF

            // Log.d (TAG, "Result! Frame: " + result.getLocation() +
            //        " mapped to screen:" + detectionScreenRect);
            // screenRects.add(new Pair<>(result.getConfidence(), detectionScreenRect));

            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                Log.d(TAG, "Degenerate rectangle! " + detectionFrameRect);
                continue;
            }

            FaceInfo faceInfo = new FaceInfo();
            faceInfo.name       = result.getTitle();
            faceInfo.confidence = result.getConfidence();
            faceInfo.location   = result.getLocation();
            faceInfos.add (faceInfo);
        }

        // Log.d (TAG, "faceInfos = " + faceInfos);
        // sleep (10000);                 // test tracking
        m_computingDetection = false; // done with prediction computation

        // Log.d (TAG, "done calling Prediction, m_computingDetection =  " + m_computingDetection);
        return faceInfos;
    }
    
    
    /**
     * Detect and recognize faces
     * This function should be run in another thread.  Use this function for no tracking.
     * @param rgbFrameBitmap image data
     * @return A list of faces and their meta info (see FaceInfo). If
     * no face is detected, return either null or empty list.
     */
    public List<FaceInfo> PredictFaces (final Bitmap rgbFrameBitmap) {
        if (m_useTracking) {
            Log.d (TAG, "Please use PredictFacesWithTracking()");
            return null;
        }
        return PredictFaces (rgbFrameBitmap, 0, null);
    }

    /**
     * Get the profile picture
     * @param ctx Android activity context (for accessing assets folder)
     * @param faceName the name of the face (see FaceInfo.name)
     * @return the profile picture or null if the picture is not available
     * @throws IOException
     */
    public Bitmap GetProfilePic (Context ctx, String faceName)
        throws IOException {
        // Attempt to open the zip archive
        Log.d (TAG, "Uncompressing data for...");
        // ZipInputStream inputStream = new ZipInputStream (ctx.getAssets().open("profiles.zip"));
        ZipInputStream inputStream = new ZipInputStream
            (new FileInputStream (new File(FaceIdFileUtils.getProfilePath())));
        String profileFileName = String.format ("profiles/%s/profile.jpg", faceName);
        Log.d (TAG, "looking for " + profileFileName);
        Bitmap bMap = null;
        
        // Loop through all the files and folders in the zip archive (but there should just be one)
        for (ZipEntry entry = inputStream.getNextEntry(); entry != null;
             entry = inputStream.getNextEntry()) {
            // Log.d (TAG, "Zip entry = " + entry.getName());

            // Skip incorrect profile
            if (!entry.getName().equals(profileFileName)) {
                continue;
            }

            // Note getSize() returns -1 when the zipfile does not have the size set
            long zippedFileSize = entry.getSize();
            Log.d (TAG, "size is " + zippedFileSize);
            ByteArrayOutputStream bOutput = new ByteArrayOutputStream ((int) zippedFileSize);
            
            // Write the contents
            int count = 0;
            final int BUFFER = 8192;
            byte[] data = new byte[BUFFER];
            while ((count = inputStream.read(data, 0, BUFFER)) != -1) {
                bOutput.write(data, 0, count);
            }
            bOutput.close();
            byte b [] = bOutput.toByteArray();
            bMap = BitmapFactory.decodeByteArray (b, 0, (int)zippedFileSize);
            
            inputStream.closeEntry();
            break;
        }
        inputStream.close();
        return bMap;
    }

    
    /**
     * Is tracking enabled?
     * @return True if tracking is enabled
     */
    public boolean GetTrackingEnabled () {
        return m_useTracking;
    }

    
    /**
     * Overlay the detected faces onto the image
     * @param mutableBitmap image to overlay to (must be mutable)
     * @param faceInfos The list of detected/recognized faces (see PredictFaces())
     * @param textSize Font size, dependig on the image resolution.
     * For 640x480 images, 24 is a good value to use.
     * @param color The color of overlay to use.  Try android.graphics.Color.RED
     * @return True if mutableBitmap has been modified
     */
    public boolean OverlayFaceIDs (Bitmap mutableBitmap,
                                   List<FaceInfo> faceInfos,
                                   final float textSize,
                                   int color) {
        // must pass a mutable object ready to be modified
        
        if (!mutableBitmap.isMutable())
            {
                Log.d (TAG, "mutableBitmap not mutable");
                return false;
            }

        if (faceInfos == null)
            {
                Log.d (TAG, "faceInfos is null");
                return false;
            }
        
        Canvas canvas = new Canvas (mutableBitmap); // modify mutableBitmap

        // RectF == left, top, right bottom
        final Paint boxPaint = new Paint();
        // boxPaint.setColor (Color.RED);
        boxPaint.setColor (color);
        boxPaint.setAlpha (150);
        boxPaint.setStyle (Paint.Style.STROKE);
        // boxPaint.setStrokeWidth (6.0f);
        int borderWith = (int) (textSize / 8);
        if (borderWith < 6)
            borderWith = 6;
        boxPaint.setStrokeWidth (borderWith);

        BorderedText borderedText;
        // borderedText = new BorderedText (16.0f);
        borderedText = new BorderedText (textSize);
        
        for (final FaceInfo faceInfo : faceInfos) {
            RectF trackedPos = faceInfo.location;
            // Log.d (TAG, "trackedPos = " + trackedPos);
            canvas.drawRoundRect (trackedPos, 0, 0, boxPaint);
            String labelString = String.format ("%s %.2f", faceInfo.name, faceInfo.confidence);
            borderedText.drawText (canvas, trackedPos.left, trackedPos.bottom, labelString);
        }

        // final BitmapFactory.Options options = new BitmapFactory.Options();
        // options.inMutable = true;
        // return BitmapFactory.decodeByteArray(resultDecoded, 0, resultDecoded.length,options);
        return true;
    }


    /**
     * Overlay the tracked faces
     * This function has no effect if tracking is disabled.
     * @param canvas Android canvas to draw to (see android.media.ImageReader.OnImageAvailableListener)
     */
    public void DrawTrackingOverlay (final Canvas canvas) {
//        final boolean rotated = true; // TC 2021-04-05 (Mon) -- TBF

        if (m_useTracking && m_initialized)
            {
                // Log.d (TAG, "canvas height = " + canvas.getHeight()); // eg. 2076
                // Log.d (TAG, "canvas width = " + canvas.getWidth()); // eg. 1080
                m_tracker.draw (canvas);
            }
        // final float multiplier =
        //     Math.min (canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
        //               canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
    }

    
    /**
     * Update tracked faces
     * For smooth tracking, this function should be called for each incoming frame.
     * If trackig is disabled, this function does nothing.
     * @param luminance Luminance data used for tracking
     * @param timestamp The frame number, which should increase at the frame rate
     */
    public void TrackFaces (final byte[] luminance,
                            final long   timestamp) {

        /*
         * 1.) Take care of tracking
         */
        if (m_useTracking && m_initialized) {
            // ++m_timestamp;

            assert (luminance != null); // TBF
            // Log.d (TAG, "calling onFrame");
            // update the tracked result at framerate and at preview resolution
            m_tracker.onFrame (m_curPreviewWidth,
                               m_curPreviewHeight,
                               m_curPreviewWidth, // getLuminanceStride (), TBF
                               m_sensorOrientation, // sensorOrientation,  TBF
                               luminance,
                               timestamp);
            // trackingOverlay.postInvalidate();
        }
    }

    
    
    /*************************************
     * helper functions
     ************************************/

    private void SetTransforms (int previewWidth, int previewHeight) {
        // We are setting the frame <-> crop transform
        // frame = preview
        Log.d (TAG, "Calling SetTransforms");
        m_frameToCropTransform = ImageUtils.getTransformationMatrix
            (previewWidth, previewHeight, CROP_SIZE, CROP_SIZE, m_sensorOrientation, false);

        m_cropToFrameTransform = new Matrix();
        m_frameToCropTransform.invert (m_cropToFrameTransform);

        m_curPreviewWidth = previewWidth;
        m_curPreviewHeight = previewHeight;
    }
}
