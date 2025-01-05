/*
 * Copyright 2020 PAR Government Systems
 *
 * Unlimited Rights:
 * PAR Government retains ownership rights to this software.  The Government has Unlimited Rights
 * to use, modify, reproduce, release, perform, display, or disclose this
 * software as identified in the purchase order contract. Any
 * reproduction of computer software or portions thereof marked with this
 * legend must also reproduce the markings. Any person who has been provided
 * access to this software must be aware of the above restrictions.
 */

package com.atakmap.android.missionapi.net.http;

import com.atakmap.android.missionapi.data.AuthManager;
import com.atakmap.android.missionapi.data.FeedManager;
import com.atakmap.android.missionapi.data.TAKServerVersion;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.net.offline.OfflineRequest;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.foxykeep.datadroid.requestmanager.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * In an ongoing effort to simplify this awful mess, this class represents
 * a generic mission request
 */
public abstract class AbstractRequest {

    private static final String TAG = "AbstractRequest";

    // API version
    protected final TAKServerVersion _serverVersion;

    // The server connect string (i.e. yeti.pargovernment.com:8443:ssl)
    protected final String _serverNCS;

    // The feed name
    protected final String _feedName;

    // The feed UID
    protected final String _feedUID;

    // Access token
    protected final String _token;

    // The notification ID (usually based on the feed UID)
    protected final int _notificationID;

    // The creation time of this request
    protected final long _creationTime;

    // Offline cache of this request
    // Used for error resolution in case the request fails
    protected final List<OfflineRequest> _offlineReqs = new ArrayList<>();

    // Extra data for custom use
    protected final Map<String, Object> _extras = new HashMap<>();

    protected AbstractRequest(TAKServerVersion serverVersion, String serverNCS,
            String feedName, String feedUID, long creationTime, String token) {
        _serverVersion = serverVersion;
        _serverNCS = serverNCS;
        _feedName = feedName;
        _feedUID = feedUID;
        _notificationID = FeedUtils.getFeedNotificationId(feedUID);
        _creationTime = creationTime;
        _token = token;
    }

    protected AbstractRequest(String serverNCS, String feedName,
            String feedUID, long creationTime) {
        this(TAKServerVersion.fromNCS(serverNCS), serverNCS,
                feedName, feedUID, creationTime,
                AuthManager.getAccessToken(feedUID));
    }

    protected AbstractRequest(String serverNCS, String feedName,
                              String feedUID) {
        this(serverNCS, feedName, feedUID, new CoordinatedTime()
                .getMilliseconds());
    }

    protected AbstractRequest(Feed feed) {
        this(feed.serverNCS, feed.name, feed.uid);
    }

    protected AbstractRequest(JSONObject json) throws JSONException {
        this(new TAKServerVersion(json.getString("ServerVersion"),
                        json.getInt("APIVersion")),
                json.getString("ServerConnectString"),
                json.getString("FeedName"),
                json.getString("FeedUID"),
                json.getLong("CreationTime"),
                json.has("Token") ? json.getString("Token") : null);

        if (json.has("OfflineRequests")) {
            Feed feed = getFeed();
            JSONArray arr = json.getJSONArray("OfflineRequests");
            for (int i = 0; i < arr.length(); i++)
                _offlineReqs.add(new OfflineRequest(feed, arr.getJSONObject(i)));
        }

        if (json.has("Extras")) {
            JSONObject extras = json.getJSONObject("Extras");
            for (Iterator<String> it = extras.keys(); it.hasNext(); ) {
                String key = it.next();
                _extras.put(key, extras.get(key));
            }
        }
    }

    public boolean isValid() {
        return !FileSystemUtils.isEmpty(getServerConnectString()) &&
                !FileSystemUtils.isEmpty(getFeedName()) &&
                !FileSystemUtils.isEmpty(getFeedUID());
    }

    /**
     * Get the server connect string in host:port:proto format
     * @return Server net connect string
     */
    public String getServerConnectString() {
        return _serverNCS;
    }

    /**
     * Convert the connect string to a URL
     * @return Server URL
     */
    public String getServerURL() {
        return FeedUtils.NCStoURL(getServerConnectString());
    }

    /**
     * Check if the server supports a certain feature
     * @param supportVersion Version the feature was added
     * @return True if supported
     */
    public boolean checkSupport(TAKServerVersion supportVersion) {
        return _serverVersion.checkSupport(supportVersion);
    }

    /**
     * Get server missions API version
     * @return API version
     */
    public int getAPIVersion() {
        return _serverVersion.api;
    }

    /**
     * Get the feed name
     * @return Feed name
     */
    public String getFeedName() {
        return _feedName;
    }

    /**
     * Get the feed UID
     * @return Feed UID
     */
    public String getFeedUID() {
        return _feedUID;
    }

    /**
     * Get feed for this request
     * @return Feed
     */
    public Feed getFeed() {
        return FeedManager.getInstance().getFeed(getFeedUID());
    }

    /**
     * Get the UID for the creator of this request
     * XXX - This is always MapView.getDeviceUID - no idea when or why we'd be
     * publishing something on behalf of somebody else
     * @return The creator UID
     */
    public String getCreatorUID() {
        return MapView.getDeviceUid();
    }

    public int getNotificationID() {
        return _notificationID;
    }

    /**
     * Get the time this request was created
     * @return Time in milliseconds
     */
    public long getCreationTime() {
        return _creationTime;
    }

    /**
     * Get the access token (for password-protected feeds)
     * @return Token or null if N/A
     */
    public String getToken() {
        return _token;
    }

    public boolean hasToken() {
        return !FileSystemUtils.isEmpty(getToken());
    }

    /**
     * Add a cached offline request associated with this request
     * @param req Offline request
     */
    public void addOfflineRequest(OfflineRequest req) {
        _offlineReqs.add(req);
    }

    /**
     * Add a list of cached requests associated with this request
     * @param requests List of offline requests
     */
    public void addOfflineRequests(List<OfflineRequest> requests) {
        _offlineReqs.addAll(requests);
    }

    /**
     * Get the cached offline requests, if any
     * @return Offline request or null if N/A
     */
    public List<OfflineRequest> getOfflineRequests() {
        return new ArrayList<>(_offlineReqs);
    }

    /**
     * Create the actual request that is sent to the operation
     * @return Request
     */
    public Request createRequest() {
        Request r = RestManager.createRequest(getRequestType(), getFeedUID());
        r.put(RestManager.REQUEST_CLASS, getClass().getName());
        r.put(RestManager.REQUEST_JSON, toJSON().toString());
        r.put(RestManager.REQUEST_TAG, getTag());
        r.put(RestManager.REQUEST_SERVER_URL, getServerURL());
        r.put(RestManager.REQUEST_FEED_NAME, getFeedName());
        r.put(RestManager.REQUEST_FEED_UID, getFeedUID());
        String contentUID = getContentUID();
        if (!FileSystemUtils.isEmpty(contentUID))
            r.put(RestManager.REQUEST_FEED_CONTENT_UID, contentUID);
        return r;
    }

    /**
     * Convert this request to JSON - used to transport the original request
     * in and out of "datadroid"
     * @return JSON object
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("ServerVersion", _serverVersion.toString());
            json.put("APIVersion", _serverVersion.api);
            json.put("ServerConnectString", getServerConnectString());
            json.put("FeedName", getFeedName());
            json.put("FeedUID", getFeedUID());
            json.put("CreationTime", getCreationTime());
            if (hasToken())
                json.put("Token", getToken());
            List<OfflineRequest> offlineReqs = getOfflineRequests();
            if (!FileSystemUtils.isEmpty(offlineReqs)) {
                JSONArray arr = new JSONArray();
                for (OfflineRequest req : offlineReqs)
                    arr.put(req.toJSON());
                json.put("OfflineRequests", arr);
            }
            if (!_extras.isEmpty()) {
                JSONObject extras = new JSONObject();
                for (Map.Entry<String, Object> e : _extras.entrySet())
                    extras.put(e.getKey(), e.getValue());
                json.put("Extras", extras);
            }
        } catch (Exception e) {
            Log.e(getTag(), "Failed to serialize JSON", e);
        }
        return json;
    }

    @Override
    public String toString() {
        return getTag() + ": " + getFeedUID();
    }

    /**
     * Get the request type - registered by the request handler
     * @return Request type
     */
    public abstract int getRequestType();

    /**
     * Get a tag which represents this request
     * @return Request tag
     */
    public abstract String getTag();

    /**
     * Build the request endpoint (path that comes after the server URL)
     * @return Request endpoint
     */
    public String getRequestEndpoint(String... args) {
        return getMissionPath();
    }

    /**
     * Default mission request endpoint
     * @return Mission endpoint
     */
    protected String getMissionPath() {
        return "api/missions/" + getFeedName();
    }

    /**
     * Generate request body (JSON format)
     * @return Request body JSON or null to ignore
     */
    public String getRequestBody() {
        return null;
    }

    /**
     * Get response string matcher
     * @return Matcher or null to ignore matching
     */
    public String getMatcher() {
        return null;
    }

    /**
     * Get associated content UID
     * @return Content UID or null if N/A
     */
    public String getContentUID() {
        return null;
    }

    /**
     * Get the error message for this request
     * @return Error message or null to use default (not recommended)
     */
    public String getErrorMessage() {
        return null;
    }

    /**
     * Put extra data within this request
     * @param key Key name
     * @param value Value (primitives-only recommended)
     */
    public void putExtra(String key, Object value) {
        _extras.put(key, value);
    }

    /**
     * Get extra data within this request
     * @param key Key name
     * @return Value
     */
    public Object getExtra(String key) {
        return _extras.get(key);
    }

    public String getExtra(String key, String defaultVal) {
        Object o = getExtra(key);
        return o != null ? String.valueOf(o) : defaultVal;
    }

    public boolean getExtra(String key, boolean defaultVal) {
        Object o = getExtra(key);
        return o instanceof Boolean ? (Boolean) o : defaultVal;
    }

    public int getExtra(String key, int defaultVal) {
        Object o = getExtra(key);
        return o instanceof Number ? ((Number) o).intValue() : defaultVal;
    }

    public float getExtra(String key, float defaultVal) {
        Object o = getExtra(key);
        return o instanceof Number ? ((Number) o).floatValue() : defaultVal;
    }

    public double getExtra(String key, double defaultVal) {
        Object o = getExtra(key);
        return o instanceof Number ? ((Number) o).doubleValue() : defaultVal;
    }

    public long getExtra(String key, long defaultVal) {
        Object o = getExtra(key);
        return o instanceof Number ? ((Number) o).longValue() : defaultVal;
    }

    /**
     * Convert a "datadroid" request back to a Data Sync request
     * @param request Datadroid request
     * @return Data Sync request
     */
    public static <T extends AbstractRequest> T fromRequest(Request request) {
        if (request == null)
            return null;

        String cName = request.getString(RestManager.REQUEST_CLASS);
        if (FileSystemUtils.isEmpty(cName)) {
            Log.e(TAG, "Request missing class name: "
                    + request.getRequestType());
            return null;
        }

        String json = request.getString(RestManager.REQUEST_JSON);
        if (FileSystemUtils.isEmpty(json)) {
            Log.e(TAG, "Request missing JSON data: " + cName);
            return null;
        }

        try {
            Class<?> clazz = Class.forName(cName);
            Constructor<?> ctor = clazz.getConstructor(JSONObject.class);
            return (T) ctor.newInstance(new JSONObject(json));
        } catch (Exception e) {
            Log.e(TAG, "Failed to create request from JSON: " + cName, e);
        }

        return null;
    }
}
