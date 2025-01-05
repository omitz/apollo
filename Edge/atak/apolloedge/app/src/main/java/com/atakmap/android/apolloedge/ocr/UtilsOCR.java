package com.atakmap.android.apolloedge.ocr;

import java.util.HashMap;
import java.util.Map;

class UtilsOCR {
    static String curLang = null;
    static Map<String, String> langs = new HashMap<String, String>();
    static TessOCR tessOCR;

    public static Map<String, String> getLangs() {
        langs.put("eng", "English");
        langs.put("ara", "Arabic");
        langs.put("fra", "French");
        langs.put("rus", "Russian");
        return langs;
    }

}
