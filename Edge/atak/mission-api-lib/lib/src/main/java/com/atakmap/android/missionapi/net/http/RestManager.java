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

import android.os.Bundle;

import com.atakmap.android.http.rest.HTTPRequestManager;
import com.atakmap.android.http.rest.NetworkOperationManager;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.data.FeedManager;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedContent;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.delete.*;
import com.atakmap.android.missionapi.net.http.get.*;
import com.atakmap.android.missionapi.net.http.put.*;
import com.atakmap.android.missionapi.notifications.RecentActivity;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.comms.NetConnectString;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager.ConnectionError;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestListener;
import com.foxykeep.datadroid.service.RequestService;

import org.apache.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manager for REST operations
 */
public class RestManager implements RequestListener {

    private static final String TAG = "RestManager";
    private static final String SALT = "MissionAPI" + TAG;

    public static final String MATCHER_MISSION = "Mission";
    public static final String MATCHER_MISSION_SUB = "MissionSubscription";
    public static final String MATCHER_RESOURCE = "Resource";
    public static final String MATCHER_LOGENTRY = "com.bbn.marti.sync.model.LogEntry";

    public static final String REQUEST_CLASS = "request_class";
    public static final String REQUEST_TAG = "request_tag";
    public static final String REQUEST_JSON = "request_json";
    public static final String RESPONSE_JSON = "response_json";
    public static final String RESPONSE_HASHES = "response_hashes";

    /**
     * Provides each Data Sync REST request with a unique ID
     */
    public static final String REQUEST_OPERATION_UID = "operation_uid";

    /**
     * Time that the request was created, and subsequently place in the queue
     */
    public static final String REQUEST_CREATE_TIME = "operation_create_time";

    /**
     * Assign a key for pending REST operations
     */
    protected static final String REQUEST_PENDING_KEY = "pending_key";

    /**
     * Associates REST request with Mission UID
     */
    public static final String REQUEST_SERVER_URL = "server_url";
    public static final String REQUEST_FEED_NAME = "feed_name";
    public static final String REQUEST_FEED_UID = "feed_uid";

    /**
     * Store content UID metadata
     */
    public static final String REQUEST_FEED_CONTENT_UID = "content_uid";

    /**
     * The CoT notification type that triggered this request (null if N/A)
     */
    public static final String REQUEST_COT_NOTIFICATION = "cot_notification";

    // Operation IDs
    public final static int DELETE_FEED_LOGS,
            DELETE_FEED_ITEMS,
            DELETE_FEED_FILES,
            DELETE_EXTERNAL_DATA,
            DELETE_FEED,
            GET_FEEDS,
            GET_FEED,
            GET_FEED_SUBSCRIPTIONS,
            GET_FEED_CHANGES,
            GET_FEED_FILE,
            GET_FEED_ATTACHMENT,
            GET_FEED_LOGS,
            GET_FEED_COT,
            GET_FEED_INVITATIONS,
            GET_FEED_ROLE,
            PUT_FEED,
            PUT_FEED_SUBSCRIPTION,
            PUT_FEED_FILES,
            PUT_FEED_FILES_METADATA,
            PUT_FEED_ITEMS,
            PUT_DATA_PACKAGE,
            PUT_OFFLINE_PACKAGE,
            PUT_EXTERNAL_DATA,
            PUT_FEED_LOGS,
            PUT_FEED_KEYWORDS,
            PUT_FEED_INVITE,
            PUT_FEED_PASSWORD,
            PUT_FEED_ROLE;

    /**
     * Initialize network operation handlers
     */
    static {
        // DELETE
        DELETE_FEED_LOGS = register(new DeleteFeedLogsOperation());
        DELETE_FEED = register(new DeleteFeedOperation());
        DELETE_FEED_ITEMS = register(new DeleteFeedItemsOperation());
        DELETE_FEED_FILES = register(new DeleteFeedFilesOperation());
        DELETE_EXTERNAL_DATA = register(new DeleteExternalDataOperation());

        // GET
        GET_FEED = register(new GetFeedOperation());
        GET_FEEDS = register(new GetFeedsListOperation());
        GET_FEED_SUBSCRIPTIONS = register(new GetFeedSubsOperation());
        GET_FEED_CHANGES = register(new GetFeedChangesOperation());
        GET_FEED_FILE = register(new GetFeedFileOperation());
        GET_FEED_ATTACHMENT = register(new GetFeedAttachmentOperation());
        GET_FEED_LOGS = register(new GetFeedLogsOperation());
        GET_FEED_COT = register(new GetFeedCotOperation());
        GET_FEED_INVITATIONS = register(new GetFeedInvitesOperation());
        GET_FEED_ROLE = register(new GetFeedRoleOperation());

        // PUT
        PUT_FEED = register(new PutFeedOperation());
        PUT_FEED_FILES = register(new PutFeedFilesOperation());
        PUT_FEED_FILES_METADATA = register(new PutFeedFilesMetadataOperation());
        PUT_FEED_ITEMS = register(new PutFeedItemsOperation());
        PUT_DATA_PACKAGE = register(new PutDataPackageOperation());
        PUT_OFFLINE_PACKAGE = register(new PutOfflinePackageOperation());
        PUT_EXTERNAL_DATA = register(new PutExternalDataOperation());
        PUT_FEED_LOGS = register(new PutFeedLogsOperation());
        PUT_FEED_KEYWORDS = register(new PutFeedKeywordsOperation());
        PUT_FEED_SUBSCRIPTION = register(new PutFeedSubscriptionOperation());
        PUT_FEED_INVITE = register(new PutFeedInviteOperation());
        PUT_FEED_PASSWORD = register(new PutFeedPasswordOperation());
        PUT_FEED_ROLE = register(new PutFeedRoleOperation());
    }

    protected static int register(RequestService.Operation handler) {
        return NetworkOperationManager.register(handler.getClass().getName(),
                handler, SALT);
    }

    private static RestManager _instance;

    /**
     * Set the singleton instance used when calling {@link #getInstance()}
     * @param manager REST manager instance or null to use default
     */
    public static void setInstance(RestManager manager) {
        _instance = manager;
    }

    public static RestManager getInstance() {
        return _instance != null ? _instance : (_instance = new RestManager());
    }

    // Track outstanding REST operations
    protected final Map<String, PendingRestOperation> _pendingRestOps;

    // Requests in progress
    protected final List<Request> _currentRequests;

    // Content requests that have error'd which should be executed on reconnect
    protected final Map<String, Request> _errorRequests;

    // Listeners
    protected final ConcurrentLinkedQueue<RequestListener> _reqListeners;
    protected final Map<String, RequestListener> _mappedListeners;

    public RestManager() {
        _pendingRestOps = new HashMap<>();
        _currentRequests = new ArrayList<>();
        _errorRequests = new HashMap<>();
        _reqListeners = new ConcurrentLinkedQueue<>();
        _mappedListeners = new HashMap<>();
    }

    public void dispose() {
        synchronized (_pendingRestOps) {
            _pendingRestOps.clear();
        }
        synchronized (_currentRequests) {
            _currentRequests.clear();
        }
        synchronized (_reqListeners) {
            _reqListeners.clear();
            _mappedListeners.clear();
        }
    }

    /**
     * Execute REST request
     * @param r Request to execute
     * @param listener Response listener for this request (null to ignore)
     * @return True if started
     */
    public boolean executeRequest(Request r, RequestListener listener) {
        MapView mv = MapView.getMapView();
        if (mv == null)
            return false;
        if (listener != null) {
            synchronized (_reqListeners) {
                _mappedListeners.put(r.getString(REQUEST_OPERATION_UID),
                        listener);
            }
        }
        onOperationStarting(r, r.getString(REQUEST_FEED_UID));
        HTTPRequestManager.from(mv.getContext()).execute(r, this);
        return true;
    }

    public boolean executeRequest(Request r) {
        return executeRequest(r, null);
    }

    public boolean executeRequest(AbstractRequest req, RequestListener listener) {
        return executeRequest(req.createRequest(), listener);
    }

    public boolean executeRequest(AbstractRequest req) {
        return executeRequest(req, null);
    }

    protected void onOperationStarting(Request request, String missionUid) {
        String tag = request.getString(REQUEST_TAG);
        String uid = request.getString(REQUEST_FEED_CONTENT_UID);
        if (FileSystemUtils.isEmpty(uid)) {
            Log.d(TAG, "onOperationStarting: " + missionUid + " - " + tag);
        } else
            Log.d(TAG, "onOperationStarting: " + missionUid + " - " + tag + " - " + uid);
        //Thread.dumpStack();
        synchronized (_currentRequests) {
            _currentRequests.add(request);
        }
    }

    /**
     * Get the number of busy downloads
     * @param feed Associated feed (null if N/A)
     * @param contentUID Content UID (null to get all)
     * @return Number of busy downloads
     */
    public List<Request> getContentRequests(Feed feed, String contentUID) {
        List<Request> ret = new ArrayList<>();
        for (Request r : getCurrentRequests()) {
            String cUID = r.getString(REQUEST_FEED_CONTENT_UID);
            String feedUID = r.getString(REQUEST_FEED_UID);
            if (FileSystemUtils.isEmpty(cUID))
                continue;
            if (contentUID != null && !cUID.equals(contentUID))
                continue;
            if (feed != null && !FileSystemUtils.isEquals(feedUID, feed.getUID()))
                continue;
            ret.add(r);
        }
        return ret;
    }

    public List<Request> getContentRequests(String contentUID) {
        return getContentRequests(null, contentUID);
    }

    /**
     * Get the list of active requests
     * @return Active requests
     */
    public List<Request> getCurrentRequests() {
        synchronized (_currentRequests) {
            return new ArrayList<>(_currentRequests);
        }
    }

    /**
     * Check if a certain operation is pending
     * @param keys Keys which make up the UID
     * @return True if pending
     */
    public boolean isPendingOperation(String... keys) {
        synchronized (_pendingRestOps) {
            return _pendingRestOps.containsKey(getPendingUid(keys));
        }
    }

    /**
     * Get a pending operation based on its UID
     * @param uid UID
     * @return Pending REST operation
     */
    public PendingRestOperation getPendingOperation(String uid) {
        synchronized (_pendingRestOps) {
            return _pendingRestOps.get(uid);
        }
    }

    /**
     * Check if a certain operation is pending
     * @param feed Associated feed
     * @param content Feed content
     * @param opType Operation type
     * @return True if pending
     */
    public boolean isPendingOperation(Feed feed, FeedContent content,
                                      PendingRestOperation.OpType opType) {
        if (content instanceof FeedFile)
            return isPendingOperation(feed.getUID(), opType.toString(),
                    ((FeedFile) content).getHash());
        return isPendingOperation(feed.getUID(), opType.toString(),
                content.getUID());
    }

    /**
     * Add a pending REST operation
     *
     * @param uid Pending operation UID
     * @param feedUID Feed UID
     * @param title Title of pending request
     * @param request Request
     */
    public boolean addPending(final String uid, String title,
                       final PendingRestOperation.OpType opType,
                       final String feedUID, final Request request) {
        if (FileSystemUtils.isEmpty(title))
            title = "item";

        final String key = getPendingUid(feedUID, opType.toString(), uid);
        synchronized (_pendingRestOps) {
            if (_pendingRestOps.containsKey(key)) {
                Log.w(TAG, "Cannot initiate duplicate REST op: " + key,
                        new Exception());
                /*RecentActivity.notifyError(null,
                        R.string.mn_sync_error,
                        R.string.mn_error_op_running);*/
                return false;
            }

            _pendingRestOps.put(key, new PendingRestOperation(feedUID,
                    opType, title, uid, request));
        }

        //store the pending key for removal upon completion
        request.put(REQUEST_PENDING_KEY, key);
        Log.d(TAG, "addPending: " + key + ".   " + request);
        return true;
    }

    /**
     * Remove a pending REST op
     *
     * @param request Request
     * @return true if it was processed/removed
     */
    protected boolean removePending(final Request request) {
        if (request == null) {
            Log.w(TAG, "removePending request invalid");
            return false;
        }

        final String key = request.getString(REQUEST_PENDING_KEY);
        if (FileSystemUtils.isEmpty(key)) {
            //expected, not all requests are tracked as pending
            return false;
        }

        PendingRestOperation pending;
        synchronized (_pendingRestOps) {
            pending = _pendingRestOps.remove(key);
        }

        if (pending != null) {
            Log.d(TAG, "removePending: " + key);
            return true;
        } else {
            Log.d(TAG, "failed to removePending: " + key);
            return false;
        }
    }

    /**
     * Add a request listener (DataDroid)
     * @param r Request listener
     */
    public void addRequestListener(RequestListener r) {
        synchronized (_reqListeners) {
            _reqListeners.add(r);
        }
    }

    public void removeRequestListener(RequestListener r) {
        synchronized (_reqListeners) {
            _reqListeners.remove(r);
        }
    }

    /**
     * Get all listeners for a given request
     * @param r Request
     * @return List of request listeners
     */
    protected List<RequestListener> getRequestListeners(Request r) {
        List<RequestListener> listeners = new ArrayList<>();
        synchronized (_reqListeners) {
            RequestListener l = _mappedListeners.remove(r.getString(
                    REQUEST_OPERATION_UID));
            if (l != null)
                listeners.add(l);
            listeners.addAll(_reqListeners);
        }
        return listeners;
    }

    public void onError(Feed feed, Request request, ConnectionError error,
                        Bundle requestBundle) {
        // Extend and implement here if needed
    }

    protected void onError(Request req, String message, ConnectionError ce,
                         Bundle bundle) {
        Log.e(TAG, "Mission Operation Failed: " + message + ", "
                + (req == null ? "" : req.getString(REQUEST_TAG)));
        removePending(req);
        synchronized (_currentRequests) {
            _currentRequests.remove(req);
        }

        Feed feed = getFeed(req);

        // Notify listeners first
        onError(feed, req, ce, bundle);

        String title = null;
        AbstractRequest r = AbstractRequest.fromRequest(req);
        if (r != null)
            title = r.getErrorMessage();

        // Build failure notification
        if (title == null) {
            title = StringUtils.getString(R.string.mn_sync_error);
            if (feed != null)
                title += ": " + feed.name;
        }

        RecentActivity.notifyError(feed != null ? feed.getUID() : null,
                title, message);
    }

    protected void onContentError(Feed feed, Request req) {
        if (feed == null || req == null)
            return;

        String contentUID = req.getString(REQUEST_FEED_CONTENT_UID);
        if (FileSystemUtils.isEmpty(contentUID))
            return;

        FeedContent content = feed.getContent(contentUID);
        if (content == null)
            return;

        synchronized (_errorRequests) {
            _errorRequests.put(contentUID, req);
        }
    }

    public void retryContentErrorRequests() {
        List<Request> requests;
        synchronized (_errorRequests) {
            requests = new ArrayList<>(_errorRequests.values());
            _errorRequests.clear();
        }
        for (Request r : requests)
            executeRequest(r);
    }

    protected String getStatusError(ConnectionError ce) {
        if (ce == null)
            return "";
        int c = ce.getStatusCode();
        switch (c) {
            case HttpStatus.SC_UNAUTHORIZED:
            case HttpStatus.SC_FORBIDDEN:
                return StringUtils.getString(R.string.mn_server_error_forbidden, c);
            case HttpStatus.SC_REQUEST_TIMEOUT:
            case HttpStatus.SC_GATEWAY_TIMEOUT:
                return StringUtils.getString(R.string.mn_server_error_timeout, c);
            case HttpStatus.SC_NOT_FOUND:
                return StringUtils.getString(R.string.mn_server_error_404);
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                return StringUtils.getString(R.string.mn_server_error_internal);
            default:
                return NetworkOperation.getErrorMessage(ce);
        }
    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {

        // Note, this handler merged in from previous separate handlers:
        // DataSyncMapOverlay, MissionHierarchyListItem, DataSyncPublisherHelper, DataSyncServerListener

        // Removing from the pending requests set
        removePending(request);

        // Remove from requests list
        synchronized (_currentRequests) {
            _currentRequests.remove(request);
        }

        // Notify listeners
        for (RequestListener r : getRequestListeners(request))
            r.onRequestFinished(request, resultData);
    }

    @Override
    public void onRequestConnectionError(Request request, ConnectionError ce) {

        for (RequestListener r : getRequestListeners(request))
            r.onRequestConnectionError(request, ce);

        Feed feed = getFeed(request);

        onError(request, getStatusError(ce), ce, null);

        // Check and process content request
        onContentError(feed, request);
    }

    @Override
    public void onRequestDataError(Request request) {
        for (RequestListener r : getRequestListeners(request))
            r.onRequestDataError(request);
        onError(request, "Request Data Error", null, null);
    }

    @Override
    public void onRequestCustomError(Request request, Bundle bundle) {
        for (RequestListener r : getRequestListeners(request))
            r.onRequestCustomError(request, bundle);
        onError(request, "Request Custom Error", null, bundle);
    }

    /* STATIC METHODS */

    public static Request createRequest(int type, String missionUid) {
        Request request = new Request(type);
        request.put(REQUEST_FEED_UID, missionUid);
        request.put(REQUEST_OPERATION_UID, getPendingUid());
        request.put(REQUEST_CREATE_TIME, new CoordinatedTime()
                .getMilliseconds());
        return request;
    }

    protected static String getPendingUid(String... strings) {
        return PendingRestOperation.generateUID(strings);
    }

    /**
     * Get mission for the specified REST request
     * @param request Request
     * @return Feed
     */
    public static Feed getFeed(Request request) {
        if (request == null)
            return null;
        String missionUid = request.getString(REQUEST_FEED_UID);
        if (!FileSystemUtils.isEmpty(missionUid)) {
            return FeedManager.getInstance().getFeed(missionUid);
        } else {
            Log.w(TAG,
                    "Mission UID not set for type: " + request.getRequestType()
                            + ", " + request.getString(REQUEST_OPERATION_UID));
        }
        return null;
    }

    /**
     * Get mission data from JSON string
     * @param r Abstract request
     * @param resJSON Bundle containing result
     * @return Mission data
     */
    public static Feed getFeed(AbstractRequest r, JSONData resJSON) {
        List<Feed> ret = getFeeds(r, resJSON);
        return FileSystemUtils.isEmpty(ret) ? null : ret.get(0);
    }

    public static Feed getFeed(AbstractRequest r, String resJSON) {
        return getFeed(r, new JSONData(resJSON, true));
    }

    public static Feed getFeed(AbstractRequest r, Bundle res) {
        return getFeed(r, res.getString(RESPONSE_JSON));
    }

    /**
     * Get list of missions from server response
     * @param r Abstract request
     * @param resJSON Bundle containing result
     * @return List of mission data
     */
    public static List<Feed> getFeeds(AbstractRequest r, JSONData resJSON) {
        List<Feed> ret = new ArrayList<>();
        try {
            JSONData data = resJSON.getChild("data");
            if (data != null) {
                if (data.isArray()) {
                    // List of feeds
                    for (JSONData d : data) {
                        Feed feed = getFeed(d);
                        if (feed != null)
                            ret.add(feed);
                    }
                } else if (data.isObject()) {
                    // A single feed
                    Feed feed = getFeed(data);
                    if (feed != null)
                        ret.add(feed);
                }
            }
            NetConnectString ncs = NetConnectString.fromString(
                    r.getServerConnectString());
            for (Feed feed : ret)
                feed.setServer(ncs);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize feed", e);
        }
        return ret;
    }

    public static List<Feed> getFeeds(AbstractRequest r, String resJSON) {
        return getFeeds(r, new JSONData(resJSON, true));
    }

    public static List<Feed> getFeeds(AbstractRequest r, Bundle res) {
        return getFeeds(r, res.getString(RESPONSE_JSON));
    }

    protected static Feed getFeed(JSONData d) {
        try {
            return Feed.create(d);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize feed: " + d);
            return null;
        }
    }
}
