package com.atakmap.android.missionapi;

import android.os.Bundle;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.data.AuthManager;
import com.atakmap.android.missionapi.data.TAKServerVersion;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedChange;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.get.GetFeedChangesRequest;
import com.atakmap.android.missionapi.net.http.get.GetFeedFileRequest;
import com.atakmap.android.missionapi.net.http.get.GetFeedRequest;
import com.atakmap.android.missionapi.net.http.get.GetFeedSubsRequest;
import com.atakmap.android.missionapi.net.http.get.GetFeedsListRequest;
import com.atakmap.android.missionapi.net.http.listener.AbstractRequestListener;
import com.atakmap.android.missionapi.net.http.put.PutFeedSubscriptionRequest;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Testing for getting the list of feeds and downloading a random feed from that list
 */
public class GetFeedTest extends AbstractHTTPTest {

    private static final String TAG = "GetFeedTest";

    // List of feeds from server
    private List<Feed> _feeds;

    // Randomly picked feed
    private Feed _feed;

    @Test
    public void getFeedTest() {

        // TEST 1 - Get feeds list //
        getFeedsList();

        // TEST 2 - Get random feed //
        getRandomFeed();

        // TEST 3 - Subscribe to random feed //
        subscribeFeed();

        // TEST 4 - Make sure this user is part of the feed //
        getUsers();

        // TEST 5 - Get feed change log //
        getChangeLog();

        // TEST 6 - Get feed user logs //
        //getUserLogs();

        // TEST 7 - Get a random file from the feed (if there is one) //
        getRandomFile();

        // TEST 8 - Unsubscribe from feed //
        unsubscribeFeed();
    }

    /**
     * Get the list of feeds
     */
    private void getFeedsList() {

        // New request for retrieving list of feeds from the server
        GetFeedsListRequest req = new GetFeedsListRequest(_server);

        // Ignore password-protected feeds for this test
        req.includePasswordFeeds(false);

        // Execute request and wait for a response
        executeRequest(req, new AbstractRequestListener() {
            @Override
            public void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode) {

                // Check for server error
                assertOk("Get feeds list", responseCode);

                // Store list of feeds for later requests
                _feeds = RestManager.getFeeds(request, resultData);
            }
        });
    }

    /**
     * Get a random feed from the list and use as our test feed
     */
    private void getRandomFeed() {

        // Make sure feeds list isn't empty
        assertNotEquals("Feeds list is empty", 0, _feeds.size());

        // Random feed
        _feed = _feeds.get(_random.nextInt(_feeds.size()));

        // Set the password used to access this feed
        // This must be called before requesting any data from the feed
        // In this example we're ignoring password protected feeds so this isn't needed
        //AuthManager.savePassword(_feed, "<password>");

        // New request for retrieving more detailed feed data
        GetFeedRequest req = new GetFeedRequest(_feed);

        // Execute request and wait for a response
        executeRequest(req, new AbstractRequestListener() {
            @Override
            public void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode) {

                // Check for server error
                assertOk("Get feed data", responseCode);

                // Get feed from result data
                _feed = RestManager.getFeed(request, resultData);

                // Make sure the feed can be parsed
                assertNotNull(_feed);
            }
        });
    }

    /**
     * Subscribe to the random feed we picked
     */
    private void subscribeFeed() {

        // New request for subscribing to a feed
        PutFeedSubscriptionRequest req = new PutFeedSubscriptionRequest(_feed, true, false);

        // Execute subscription request and wait for response
        executeRequest(req, new AbstractRequestListener() {
            @Override
            public void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode) {

                // Make sure the feed subscription succeeded
                assertOk("Feed subscription", responseCode);

                // Read and save access token (only for TAK server v1.3.13 and newer)
                if (TAKServerVersion.checkSupport(_server, TAKServerVersion.SUPPORT_AUTH)) {

                    // Parse response as JSON
                    JSONObject json = null;
                    try {
                        json = getJSONData("Failed to parse subscription response", resultData);
                        json = json.getJSONObject("data");
                    } catch (Exception e) {
                        fail("Failed to read subscription response data");
                    }

                    // Make sure JSON has response data
                    assertNotNull("Subscription response data is missing", json);

                    // Save access token for later requests
                    AuthManager.saveAccessToken(_feed, json);
                }
            }
        });
    }

    /**
     * Get the list of feed users and make sure we're in it
     */
    private void getUsers() {

        // New request for retrieving feed users
        GetFeedSubsRequest req = new GetFeedSubsRequest(_feed, false);

        // Execute users request
        executeRequest(req, new AbstractRequestListener() {
            @Override
            public void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode) {

                // Make sure the request succeeded
                assertOk("Feed users", responseCode);

                // Parse response
                boolean isUser = false;
                try {
                    JSONObject json = getJSONData("Failed to parse users response", resultData);
                    JSONArray arr = json.getJSONArray("data");

                    // Loop through users and make sure this device is in there
                    for (int i = 0; i < arr.length(); i++) {
                        String clientUID = arr.getString(i);
                        if (clientUID.equals(MapView.getDeviceUid())) {
                            isUser = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    fail("Failed to read users response data");
                }

                assertTrue("Not part of subscribed feed \"" + _feed.name + "\"", isUser);
            }
        });
    }

    /**
     * Get the change log for the feed
     */
    private void getChangeLog() {

        // New request for getting change log
        GetFeedChangesRequest req = new GetFeedChangesRequest(_feed, 0, false);

        // Execute change log request
        executeRequest(req, new AbstractRequestListener() {
            @Override
            public void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode) {

                // Check that request succeeded
                assertOk("Change log", responseCode);

                // Parse change log response
                JSONObject json = getJSONData("Failed to get change log response", resultData);

                // Parse change list
                List<FeedChange> changes = null;
                try {
                    JSONData data = new JSONData(json, true);
                    changes = data.getList("data", _feed, FeedChange.class);
                } catch (Exception e) {
                    fail("Failed to serialize JSON results");
                }

                // Make sure the changes aren't empty (should always be at least 1 change)
                assertNotEquals("Change log is empty", 0, changes.size());
            }
        });
    }

    /**
     * Get a random file from the feed if one is available
     */
    private void getRandomFile() {

        // See if there's a file we can pick
        List<FeedFile> files = _feed.getFiles();
        if (files.isEmpty()) {
            Log.d(TAG, _feed.name + " has no files - skipping test");
            return;
        }

        // Get a random file from the feed
        FeedFile file = files.get(_random.nextInt(files.size()));

        // New request to obtain the file
        GetFeedFileRequest req = new GetFeedFileRequest(_feed, file, true);

        // Save file to the test directory
        req.setDestinationDirectory(TestUtils.getTestDir());

        // Execute request
        executeRequest(req, new AbstractRequestListener() {
            @Override
            public void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode) {

                // Check that request succeeded
                assertOk("Get file request", responseCode);

                // Get the path of the downloaded file
                String path = resultData.getString(RestManager.RESPONSE_JSON);

                // Check that file is valid and exists
                assertTrue("File path is missing", FileSystemUtils.isFile(path));

                // Now delete the file
                FileSystemUtils.delete(path);
            }
        });
    }

    /**
     * Unsubscribe from the feed
     */
    private void unsubscribeFeed() {

        // New request for subscribing to a feed
        PutFeedSubscriptionRequest req = new PutFeedSubscriptionRequest(_feed, true, false);

        // Execute subscription request and wait for response
        executeRequest(req, new AbstractRequestListener() {
            @Override
            public void onRequestResult(AbstractRequest request, Bundle resultData, int responseCode) {

                // Check that request succeeded
                assertOk("Feed unsubscribe", responseCode);
            }
        });
    }
}
