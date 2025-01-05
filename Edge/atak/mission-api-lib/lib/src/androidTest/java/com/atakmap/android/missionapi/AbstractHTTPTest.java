package com.atakmap.android.missionapi;

import android.os.Bundle;

import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.listener.AbstractRequestListener;
import com.atakmap.comms.TAKServer;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Helper class for HTTP tests
 */
public abstract class AbstractHTTPTest {

    private static final String TAG = "AbstractHTTPTest";

    // Default TAK server that's pulled from the list of configured servers
    // Normally the user would select their default server
    protected static TAKServer _server;

    // Random number generator
    protected static Random _random = ThreadLocalRandom.current();

    /**
     * Obtain a server for testing
     */
    @BeforeClass
    public static void start() {
        // Get a sample test server from the users list of configured servers
        _server = TestUtils.getTestServer();

        // Make sure the server is valid
        assertNotNull(_server);
    }

    /**
     * Delete the test directory on test shutdown
     */
    @AfterClass
    public static void shutdown() {
        FileSystemUtils.deleteDirectory(TestUtils.getTestDir(), false);
    }

    /**
     * Helper method for executing a request within a test class
     * This method will block the main thread until the request is finished
     *
     * Under normal circumstances you would just call this directly:
     * {@link RestManager#getInstance()#executeRequest(AbstractRequest, AbstractRequestListener)}
     *
     * @param request Request to execute
     * @param listener Request finished listener
     */
    protected void executeRequest(AbstractRequest request, final AbstractRequestListener listener) {

        // Stored result
        final RequestResult[] result = { null };

        // Use the default REST manager to execute a network request
        RestManager.getInstance().executeRequest(request, new AbstractRequestListener() {
            @Override
            public void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode) {

                // Store request result
                RequestResult rr = new RequestResult();
                rr.request = request;
                rr.resultData = resultData;
                rr.responseCode = responseCode;

                // Signal to return to the original thread
                synchronized(result) {
                    result[0] = rr;
                    result.notify();
                }
            }
        });

        // Block main thread until the request is finished
        synchronized (result) {
            while(result[0] == null) {
                try {
                    result.wait();
                } catch (InterruptedException ignored) {
                }
            }
        }

        // Notify listener on main thread
        listener.onRequestResult(result[0].request, result[0].resultData, result[0].responseCode);
    }

    // Container for request result
    private static class RequestResult {
        // Initial request
        AbstractRequest request;

        // Result data bundle
        Bundle resultData;

        // HTTP response code
        int responseCode;
    }

    /**
     * Get JSON object from result data
     * @param failMsg Fail message
     * @param resultData Result data bundle
     * @return Result JSON or null if failed
     */
    protected static JSONObject getJSONData(String failMsg, Bundle resultData) {
        try {
            return new JSONObject(resultData.getString(RestManager.RESPONSE_JSON));
        } catch (Exception e) {
            fail(failMsg);
        }
        return null;
    }

    /**
     * Assert HTTP response code matches the expected response code
     * @param msgPrefix Error message prefix
     * @param responseCode Actual response code
     * @param expectedCode Expected response code
     */
    protected static void assertCode(String msgPrefix, int responseCode, int expectedCode) {
        assertEquals(msgPrefix + " bad response code", expectedCode, responseCode);
    }

    protected static void assertOk(String msgPrefix, int responseCode) {
        assertCode(msgPrefix, responseCode, 200);
    }
}
