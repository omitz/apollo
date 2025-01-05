package com.caci.apollo.speaker_id_library;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import com.caci.apollo.speaker_id_library.MediaEditDemo.MuxHelper;

import net.razorvine.pickle.Unpickler;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.kaldi.Model;
import org.kaldi.KaldiRecognizer;
import org.kaldi.SpkModel;
import org.kaldi.Vosk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
// import java.nio.ByteBuffer;
// import java.nio.ByteOrder;
// import java.nio.file.Files;
// import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.caci.apollo.speaker_id_library.BasicLinearAlgebra.*;

public class SpeakerRecognitionModel {
    /*************************************
     * Constants 
     ************************************/
    private static String TAG = "Tommy SpkrRecogModel";
    private static String SYNC_DIR = "syncSpkrID";
    private static String ATAK_DATA_PACKAGE_DIR = "/sdcard/atak/tools/datapackage/files/"; // TBF...

    /*************************************
     * member variables
     ************************************/
    private SVC                m_svmClasifier ; // for SVM classifier
    private EnrollmentMetaInfo m_enrollmentMetaInfo;
    private String             m_version = "N/A";
    private String m_cacheDir = null;

    private Model              m_model;
    private SpkModel           m_spkModel;
    private String             m_profileZip;

    /*************************************
     * data structures
     ************************************/
    
    private class EnrollmentMetaInfo {
        List<String> m_speakers;
        Integer      m_fiftyPoint;

        EnrollmentMetaInfo (String enrollmentMetaPath) throws IOException {
            // 1.) Load pickle file
            Log.d (TAG, "loading pickle file " + enrollmentMetaPath);
            // byte[] allBytes = Files.readAllBytes (Paths.get(enrollmentMetaPath));
            File file = new File (enrollmentMetaPath);
            byte[] allBytes = FileUtils.readFileToByteArray(file);
            
            Log.d (TAG, "done loading pickle file " + enrollmentMetaPath);
            spkrIdUtils.Assert.that (allBytes.length != 0, "");
            Unpickler unpickler = new Unpickler();
            List<Object> data = (List<Object>) unpickler.loads(allBytes);
            
            // 2.) load speakers column/series
            List<String> speakers = (List<String>) data.get(0);

            // 2.) load fiftyPoint as int32
            Integer fiftyPoint = (Integer) data.get(1);

            // 4.) save to result
            m_speakers = speakers;
            m_fiftyPoint = fiftyPoint;
            Log.d (TAG, "loaded enrollmentMeta data, fiftyPoint = " + fiftyPoint);
        }
    }
    
    public class SpeakerInfo {
        public double maxScore;
        public String speakerName;
    } 
    
    
    private class RecognizerInfo {
        Model           model;
        SpkModel        spkModel;
        KaldiRecognizer rec;
        RecognizerInfo (Model _model, SpkModel _spkModel) {
            model = _model;
            spkModel = _spkModel;
            rec = new KaldiRecognizer (model, spkModel, 16000.f);
        }
    }

   

    /*************************************
     * member functions
     ************************************/

    /**
     * Preload all model files
     * @param ctx The android activity context (eg., type
     * AppCompatActivity).  Context is needed for copying embedding
     * model from the assets folder, if not already copied.  
     * @param atakDataPackageUUID The uuid of the data package.  The
     * location of the data package is assumed to be located at
     * /sdcard/atak/tools/datapackage/files/<dataPackage_uuid>/
     * (e.g. "ApolloSpeakerID-10VIP"). Note: data package is also known as
     * mission package.  Do not confuse it with "mission data feed",
     * which is a publish/subscribe synchronization service from the
     * TAK Server.
     * @throws IOException model could not be loaded
     */
    public void LoadModels (Context ctx, String atakDataPackageUUID) throws IOException {
        LoadModels (ctx, ATAK_DATA_PACKAGE_DIR + "/" + atakDataPackageUUID + "/meta_vosk",
                    ATAK_DATA_PACKAGE_DIR + "/" + atakDataPackageUUID + "/svm_vosk",
                    ATAK_DATA_PACKAGE_DIR + "/" + atakDataPackageUUID + "/profiles",
                    true);
    }

    
    /**
     * Preload all model files
     * @param ctx The android activity context (eg., type
     * AppCompatActivity).  Context is needed for copying embedding
     * model from the assets folder, if not already copied.  
     * @throws IOException model could not be loaded
     */
    public void LoadModels (Context ctx) throws IOException {
        LoadModels (ctx, SYNC_DIR + "/classifier/meta_vosk.pkl",
                    SYNC_DIR + "/classifier/svm_vosk.json",
                    SYNC_DIR + "/profiles.zip",
                    false);
    }

    
    /**
     * Preload all model files
     * @param ctx The android activity context (eg., type
     * AppCompatActivity).  Context is needed for copying embedding
     * model from the assets folder, if not already copied.  
     * @param enrollmentMetaAssetFile The enrollment meta file.  This file contains
     * the list of speakers 
     * @param classifierAssetFile The classifier file
     * @param profilesAssetFile A zip file containing profile images
     * @param useAbsolutePath set to true if enrollmentMetaAssetFile
     * and classifierAssetFile are absolute paths.  Otherwise relative
     * to the application internal directory (eg.,"/internal/storage/Android/data/com.your_app_name/files/")
     * @throws IOException model could not be loaded
     */
    public void LoadModels (Context ctx,
                            String  enrollmentMetaAssetFile,
                            String  classifierAssetFile,
                            String  profilesAssetFile,
                            boolean useAbsolutePath) throws IOException {
        
        m_cacheDir = ctx.getCacheDir().getAbsolutePath();

        Vosk.SetLogLevel(-1); // slience
        Assets assets = new Assets (ctx, SYNC_DIR);
        File assetDir = assets.syncAssets (); // copy model from apk's
                                              // assets folder to app's
                                              // internal directory
        Log.d (TAG, "Sync files in the folder " + assetDir.toString());
            
        String voskModelDir = // only english langague for now
            assetDir.toString() + "/model-android-en"; 
        String voskSpkrModelDir = assetDir.toString() + "/model-spk";


        // set the model path
        String enrollmentMetaFile;
        String classifierFile;
        String profilesFile;
        if (useAbsolutePath) {
            enrollmentMetaFile = enrollmentMetaAssetFile;
            classifierFile = classifierAssetFile;
            profilesFile = profilesAssetFile;
        } else {
            enrollmentMetaFile = assetDir.toString() + "/../" + enrollmentMetaAssetFile;
            classifierFile = assetDir.toString() + "/../" + classifierAssetFile;
            profilesFile = assetDir.toString() + "/../" + profilesAssetFile;
        }
            
        m_model = new Model (voskModelDir);
        m_spkModel = new SpkModel (voskSpkrModelDir);
            
        // String filePath = SPEAKER_ID_ENROLLMENT_META_FILE;
        Log.d (TAG, "loading speaker meta file " + enrollmentMetaFile);
        LoadEnrollmentMeta (enrollmentMetaFile);
            
        // We create and load SVM classifier
        Log.d (TAG, "loading svm model file " + classifierFile);
        LoadClassifier (classifierFile);
            
        // Log.d (TAG, "svm model loaded!");
        Log.d (TAG, "profile zip = " + profilesFile);
        m_profileZip = profilesFile;
    }

    /**
     * Get enrollment/classifier version
     * @return 7-digit sha256 hash string
     */
    public String GetVersion() {
        return m_version;
    }

    /**
     * Predict the speaker
     * @param rawAudioData The raw audio data in 16-bit, 16 khz, mono format and no header
     * @return The predicted speaker (See SpeakerInfo data type)
     */
    public SpeakerInfo PredictSpeaker (short[] rawAudioData) {

        RecognizerInfo recognizer = GetNewRecognier ();
        double[] embedding = GetEmbeddingFromAudioData (rawAudioData, recognizer);
        SpeakerInfo spkrInfo;
        if (embedding != null) {
            spkrInfo = GetPredictedSpeakerID (embedding);
        } else {
            spkrInfo = new SpeakerInfo ();
            spkrInfo.speakerName = "No speech detected";
            spkrInfo.maxScore = 0.0;
        }
//        Log.d (TAG, "speaker = " + spkrInfo.speakerName);
//        Log.d (TAG, "score = " + spkrInfo.maxScore);
        return spkrInfo;
    }


    /**
     * Extract raw audio data from an audio file stream and convert to
     * 16-bit, 16-khz and mono channel 
     * @param audioFileStream the content of a supported audio file (wav, aac, etc) (eg., new
     * FileInputStream(new File(audioFileName)).  
     * @param durationSec limit the data to extract in seconds
     * @return raw audio data, each sample is a short (16-bit); null if any error
     */
    public short[] DecodeAudioData (InputStream audioFileStream, int durationSec) {
        if (audioFileStream == null)
            return null;

        MuxHelper muxHelper = new MuxHelper();
        byte[] bytes;
        
        // We check for API, if it is >= API 23, then, we use custom MediaDataSource.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // no need to close audioFileStream explicitly
            bytes = muxHelper.decodeMusicFileStream (audioFileStream, 0, durationSec, 1, 16000);
        } else {
            // For API 21, we use regular file 
            try {
                // convert inputstrem to file
                String musicFileUrl = m_cacheDir + "/inputSpeech";
                Log.d (TAG, "musicFileUrl = " + musicFileUrl);
                FileUtils.copyToFile (audioFileStream, new File (musicFileUrl));
                audioFileStream.close (); // close explicitly here

                // extract the audio data
                bytes = muxHelper.decodeMusicFile (musicFileUrl, 0, durationSec, 1, 16000);

                // clean up 
                File tempFile = new File (musicFileUrl);
                tempFile.delete();
            } catch (Exception e) {
                Log.d(TAG, "===>DecodeAudioData API21 error");
                return null;
            }
        }

        
        // return bytesToShort (bytes);
        assert (bytes != null);
        return ByteUtils.bytesToShorts (bytes);
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
        ZipInputStream inputStream = new ZipInputStream
            (new FileInputStream(new File(m_profileZip)));
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

    
    /*************************************
     * helper functions
     ************************************/


// public static byte[] readAllBytes(Path path) throws IOException {
//         long size = size(path);
//         if (size > (long)Integer.MAX_VALUE)
//             throw new OutOfMemoryError("Required array size too large");

//         try (InputStream in = newInputStream(path)) {
//              return read(in, (int)size);
//         }
//     }


    // File file = new File("/path/file");
    // byte[] bytes = FileUtils.readFileToByteArray(file);
    
    private RecognizerInfo GetNewRecognier () {
        return new RecognizerInfo (m_model, m_spkModel);
    }

    private double[] GetEmbeddingFromAudioData (short[]        samples,
                                                RecognizerInfo recogInfo) {
        return GetEmbeddingFromAudioData (samples, recogInfo, true);
    }


    
    private double[] GetEmbeddingFromAudioData (short[]        samples,
                                               RecognizerInfo recogInfo,
                                               boolean        normalize_flg) {
        byte[] b = new byte[4096];
        // byte[] samplesBytes = shortToBytes(samples);
         byte[] samplesBytes = ByteUtils.shortsToBytes (samples);
        
        ArrayList<JSONArray> spkJsonArrays = new ArrayList<>();
        int lastStartIdx = (samplesBytes.length - b.length);
        for (int idx = 0; idx < lastStartIdx; idx += b.length) {
            System.arraycopy(samplesBytes, idx, b, 0, b.length);
            if (recogInfo.rec.AcceptWaveform(b)) {
                JSONObject resultJson = null;
                try {
                    resultJson = (JSONObject)
                        new JSONParser().parse(recogInfo.rec.Result());
                } catch (ParseException e) {
                    e.printStackTrace();
                    return null;
                }
                if (resultJson.containsKey("spk")) {
                    JSONArray jsonArray = (JSONArray) resultJson.get ("spk");
                    spkJsonArrays.add (jsonArray);
                }
            }
        }

        JSONObject resultJson = null;
        try {
            resultJson = (JSONObject)
                new JSONParser().parse(recogInfo.rec.FinalResult());
            // Log.d (TAG, "after parsing result " + resultJson);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        if (resultJson.containsKey("spk")) {
            JSONArray vecArray = (JSONArray) resultJson.get ("spk");
            spkJsonArrays.add (vecArray);
        }
        double[] avgEmbedding = ComputeAvgSpkEmbedding (spkJsonArrays, normalize_flg);
        return avgEmbedding;
    }

    private SpeakerInfo GetPredictedSpeakerID (double[] query) {
        spkrIdUtils.Assert.that (query != null, "");
        int maxIdx = m_svmClasifier.predict (query);
        // double[] proba = clf.get_predict_proba ();
        // Log.d (TAG, "proba = " + proba);
        double prob_val = m_svmClasifier.get_max_predict_proba () * 100;
        // Log.d (TAG, "max prob_val = " + prob_val);
        
        SpeakerInfo speakerInfo = new SpeakerInfo();
        speakerInfo.speakerName = m_enrollmentMetaInfo.m_speakers.get(maxIdx);
        
        speakerInfo.maxScore = RescaleScore (prob_val, m_enrollmentMetaInfo.m_fiftyPoint); 
        
        return speakerInfo;
    }

    private void LoadEnrollmentMeta (String metaFile) throws IOException {
        m_enrollmentMetaInfo = new EnrollmentMetaInfo (metaFile);
    }

    private void LoadClassifier (String classiferFile) throws IOException {
        m_svmClasifier = new SVC (classiferFile);

        Log.d (TAG, "before calling SHA256");
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance ("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            Log.d (TAG, "error ..");
            e.printStackTrace();
            throw new IOException();
        }
        Log.d (TAG, "get all data from file: " + classiferFile);
        // byte[] data = Files.readAllBytes(Paths.get(classiferFile));
        File file = new File (classiferFile);
        byte[] data = FileUtils.readFileToByteArray(file);

        Log.d (TAG, "get all byte to hex.");
        String sha256Hex = new String (bytesToHex(digest.digest(data)));
        Log.d (TAG, "sha256Hex = " + sha256Hex);
        m_version = new String (sha256Hex.substring(0,7));
        Log.d (TAG, "m_version = " + m_version);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    private double [] ComputeAvgSpkEmbedding (ArrayList<JSONArray> spkJsonArrays,
                                              boolean               normalize_flg) {
        int nVecs = spkJsonArrays.size();

        // Assert.that (nVecs > 0, "");
        if (nVecs == 0) {
            // The audio is silence
            return null;
        }
        double[] avgVec = fillData(spkJsonArrays.get(0));
            
        // we are taking the average:
        for (int idx = 1; idx < nVecs; idx++) {
            double [] vec = fillData (spkJsonArrays.get(idx));
            add (avgVec, vec);  // hopefully no overflow
        }
        if (nVecs > 1)
            scale (1.0 / nVecs, avgVec); // actual average
            
        // normalize the avg vector if needed
        if (normalize_flg)
            scale (1.0 / norm2(avgVec), avgVec);
        return avgVec;
    }

    private double lerp (double x, double x0, double x1, double y0, double y1) {
        return y0 + (x - x0) / (x1 - x0) * (y1 - y0);
    }

    private int RescaleScore (double score, int fiftyPoint) {        
        if (score < fiftyPoint) 
            return (int) lerp (score, 0, fiftyPoint, 0, 50);
        return (int) lerp (score, fiftyPoint, 100, 50, 100);
    }

    // private short[] bytesToShort(byte[] bytes) {
    //     short[] shorts = new short[bytes.length/2];
    //     ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
    //     return shorts;
    // }

    // private byte[] shortToBytes(short[] shorts) {
    //     byte[] bytes = new byte[shorts.length * 2];
    //     ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
    //     return bytes;
    // }
}
