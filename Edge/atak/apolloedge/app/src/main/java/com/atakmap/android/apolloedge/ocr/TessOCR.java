package com.atakmap.android.apolloedge.ocr;

import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;

public class TessOCR {
    private final TessBaseAPI mTess;
    public static final String ROOT =
            Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator;

    public TessOCR(String language) {
        mTess = new TessBaseAPI();
        mTess.init(ROOT, language);
    }

    public String getOCRResult(Bitmap bitmap) {
        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();
        return result;
    }

    public void onDestroy() {
        if (mTess != null) mTess.end();
    }
}

