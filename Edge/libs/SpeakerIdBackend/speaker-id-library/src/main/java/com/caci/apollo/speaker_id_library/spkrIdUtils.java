
package com.caci.apollo.speaker_id_library;
public class spkrIdUtils {
    private static String TAG = "Tommy spker-id-library";
    public static class Assert {
        public static void that(boolean condition, String message) {
            if (!condition) {
                throw new AssertionError(message);
            }
        }
    }
}
