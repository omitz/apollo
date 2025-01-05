// package pp.facerecognizer.wrapper;
package com.caci.apollo.face_id_library.wrapper;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

//import androidx.core.util.Pair;
// import pp.facerecognizer.Classifier;
// import pp.facerecognizer.env.FileUtils;
import com.caci.apollo.face_id_library.Classifier;
import com.caci.apollo.face_id_library.env.FaceIdFileUtils;

/**
 * Created by yctung on 9/26/17.
 * This is a java wrapper of LibSVM
 */

public class LibSVM {
    private static String TAG = "Tommy LibSVM";
    // private String DATA_PATH = FaceIdFileUtils.ROOT + File.separator + FaceIdFileUtils.DATA_FILE;
    // private String MODEL_PATH = FaceIdFileUtils.ROOT + File.separator + FaceIdFileUtils.MODEL_FILE;
    // private String DATA_PATH; 
    private String MODEL_PATH;

    private int index;
    private double prob;

    static {
        System.loadLibrary("jnilibsvm");
    }

    // connect the native functions
    private native void testLog(String log);
    private native void jniSvmTrain(String cmd);
    private native void jniSvmPredict(String cmd, FloatBuffer buf, int len);
    private native void jniSvmScale(String cmd, String fileOutPath);


    // public LibSVM (String model_path, String data_path) {
    //     DATA_PATH = data_path; // FaceIdFileUtils.ROOT + File.separator + FaceIdFileUtils.DATA_FILE;
    //     MODEL_PATH = model_path; // FaceIdFileUtils.ROOT + File.separator + FaceIdFileUtils.MODEL_FILE;
    // }

    public LibSVM (String model_path) {
        MODEL_PATH = model_path; // FaceIdFileUtils.ROOT + File.separator + FaceIdFileUtils.MODEL_FILE;
    }

    
    // public interfaces
    private void train(String cmd) {
        jniSvmTrain(cmd);
    }
    private void predict(String cmd, FloatBuffer buf, int len) {
        jniSvmPredict(cmd, buf, len);
    }
    private void scale(String cmd, String fileOutPath) {
        jniSvmScale(cmd, fileOutPath);
    }

    // public void train(int label, ArrayList<float[]> list) {
    //     StringBuilder builder = new StringBuilder();

    //     for (int i = 0; i < list.size(); i++) {
    //         float[] array = list.get(i);
    //         builder.append(label);
    //         for (int j = 0; j < array.length; j++) {
    //             builder.append(" ").append(j).append(":").append(array[j]);
    //         }
    //         if (i < list.size() - 1) builder.append(System.lineSeparator());
    //     }
    //     FaceIdFileUtils.appendText(builder.toString(), FaceIdFileUtils.DATA_FILE);

    //     train();
    // }

    // public void train() {
    //     String options = "-t 0 -b 1";
    //     String cmd = TextUtils.join(" ", Arrays.asList(options, DATA_PATH, MODEL_PATH));
    //     train(cmd);
    // }

    public Pair<Integer, Float> predict(FloatBuffer buffer) {
        String options = "-b 1";
        String cmd = TextUtils.join(" ", Arrays.asList(options, MODEL_PATH));

        // Log.d (TAG, "predict option is " + cmd);
        predict(cmd, buffer, Classifier.EMBEDDING_SIZE);
        return new Pair<>(index, (float) prob);
    }

    // singleton for the easy access
    private static LibSVM svm;
    // public static LibSVM getInstance(String model_path, String data_path) {
    //     if (svm == null) {
    //         svm = new LibSVM(model_path, data_path);
    //     }
    //     return svm;
    // }

    public static LibSVM getInstance(String model_path) {
        if (svm == null) {
            svm = new LibSVM(model_path);
        }
        return svm;
    }

    
    // private LibSVM() {
    //     Log.d(LOG_TAG, "LibSVM init");
    // }
}
