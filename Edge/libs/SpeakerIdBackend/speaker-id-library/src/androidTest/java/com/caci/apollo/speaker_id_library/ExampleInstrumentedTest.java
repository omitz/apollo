package com.caci.apollo.speaker_id_library;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private static String TAG = "Tommy ExampleInstTest";
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Log.d (TAG, "testing usingAppContext");
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.caci.apollo.speaker_id_library.test", appContext.getPackageName());
    }
}
