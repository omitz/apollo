package pp.facerecognizer.utils;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;


public class ColorThresholds {

    public static final float LOW_CONFIDENCE_THRESHOLD = 0.4f;
    private static final float HIGH_CONFIDENCE_THRESHOLD = 0.75f;

    private static final Map<String, Integer> COLOR_THRESHOLDS = new HashMap<String, Integer>() {
        {
            put("unknown", Color.BLUE);
            put("low", Color.YELLOW);
            put("high", Color.GREEN);
        }
    };

    public static int setColorBasedOnConfidence(float confidence) {
        // Assign box color based on confidence
        int color;
        if (confidence > HIGH_CONFIDENCE_THRESHOLD) {
            color = COLOR_THRESHOLDS.get("high");
        } else if (confidence > LOW_CONFIDENCE_THRESHOLD) {
            color = COLOR_THRESHOLDS.get("low");
        } else {
            color = COLOR_THRESHOLDS.get("unknown");
        }
        return color;
    }
}
