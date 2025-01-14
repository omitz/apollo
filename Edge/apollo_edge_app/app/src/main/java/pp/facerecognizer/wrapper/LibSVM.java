package pp.facerecognizer.wrapper;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.core.util.Pair;
import pp.facerecognizer.Classifier;
import pp.facerecognizer.utils.FileUtils;

/**
 * Created by yctung on 9/26/17.
 * This is a java wrapper of LibSVM
 */

public class LibSVM {
    private String LOG_TAG = "LibSVM";

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

    public void train(int label, ArrayList<float[]> list) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            float[] array = list.get(i);
            builder.append(label);
            for (int j = 0; j < array.length; j++) {
                builder.append(" ").append(j).append(":").append(array[j]);
            }
            if (i < list.size() - 1) builder.append(System.lineSeparator());
        }
        FileUtils.appendText(builder.toString(), FileUtils.DATA_FILE);

        train();
    }

    public void train() {
        String options = "-t 0 -b 1";
        String cmd = TextUtils.join(" ", Arrays.asList(options, FileUtils.DATA_PATH, FileUtils.MODEL_PATH));
        train(cmd);
    }

    public Pair<Integer, Float> predict(FloatBuffer buffer) {
        String options = "-b 1";
        String cmd = TextUtils.join(" ", Arrays.asList(options, FileUtils.MODEL_PATH));

        predict(cmd, buffer, Classifier.EMBEDDING_SIZE);
        return new Pair<>(index, (float) prob);
    }

    // singleton for the easy access
    private static LibSVM svm;
    public static LibSVM getInstance() {
        if (svm == null) {
            svm = new LibSVM();
        }
        return svm;
    }

    private LibSVM() {
        Log.d(LOG_TAG, "LibSVM init");
    }
}
