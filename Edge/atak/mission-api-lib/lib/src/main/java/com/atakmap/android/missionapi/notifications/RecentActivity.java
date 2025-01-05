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

package com.atakmap.android.missionapi.notifications;

import android.content.Intent;

import com.atakmap.android.missionapi.data.FeedManager;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.TAKServer;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recent activity manager
 */
public class RecentActivity {

    private static final String TAG = "RecentActivity";

    private static final Map<String, List<RecentActivity>> _raLists = new HashMap<>();
    private static IntentProvider _intentProvider;

    public final long timestamp;
    public final int iconId;
    public final String title, text;

    public RecentActivity(long timestamp, int iconId, String title,
                          String text) {
        this.timestamp = timestamp;
        this.iconId = iconId;
        this.title = title;
        this.text = text;
    }

    public RecentActivity(int iconId, String title, String text) {
        this(new CoordinatedTime().getMilliseconds(), iconId, title, text);
    }

    @Override
    public String toString() {
        return title + "," + text + "," + iconId + "," + timestamp;
    }

    /**
     * Notify the user of a custom event
     * @param uid Mission UID or server URL (null if general notification)
     * @param iconId Icon resource ID (must be ATAK-core resource)
     * @param title Notification title
     * @param message Message
     * @param archive True to archive in recent activity, false to only display notification
     */
    public static void notify(String uid, int iconId, String title,
            String message, boolean archive) {
        Feed feed = null;
        TAKServer server = null;
        if (!FileSystemUtils.isEmpty(uid)) {
            FeedManager mgr = FeedManager.getInstance();
            feed = mgr != null ? mgr.getFeed(uid) : null;
            if (feed == null && !FileSystemUtils.isEmpty(uid)) {
                try {
                    NetConnectString ncs = null;
                    if (uid.startsWith("http:") || uid.startsWith("https:"))
                        ncs = FeedUtils.URLtoNCS(uid);
                    else if (uid.split(":").length >= 2)
                        ncs = NetConnectString.fromString(uid);
                    if (ncs != null) {
                        server = FeedUtils.findServer(ncs.toString());
                        uid = ncs.toString();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse potential server URL: " + uid);
                }
            }
        }

        if (FileSystemUtils.isEmpty(uid) || server != null
                || feed != null && feed.notifications) {
            int nid = FeedUtils.getFeedNotificationId(uid);
            NotificationUtil.getInstance().postNotification(nid, iconId, title,
                    message, message, getNotificationIntent(uid),
                    true);
        } else {
            Log.w(TAG, "Failed to create notification for invalid UID: " + uid,
                    new Throwable());
        }
        if (archive)
            addRecentActivity(uid, new RecentActivity(iconId, title, message));
    }

    /**
     * Notify the user of a general event
     * @param missionUid Mission UID (null if general notification)
     * @param title Notification title
     * @param message Message
     */
    public static void notify(String missionUid, String title, String message) {
        notify(missionUid, NotificationUtil.GeneralIcon.SYNC_ORIGINAL.getID(),
                title, message, false);
    }

    /**
     * Notify the user there's been an error
     * @param missionUid Mission UID (null if general notification)
     * @param title Notification title
     * @param message Error message
     */
    public static void notifyError(String missionUid, String title,
            String message) {
        Log.e(TAG, "notifyError: " + missionUid + ", " + title + ", " + message, new Throwable());
        notify(missionUid, NotificationUtil.GeneralIcon.SYNC_ERROR.getID(),
                title, message, true);
    }

    public static void notifyError(String missionUid, int titleId, int msgId) {
        notifyError(missionUid, StringUtils.getString(titleId),
                StringUtils.getString(msgId));
    }

    /**
     * Notify the user an event has finished successfully
     * @param missionUid Mission UID (null if general notification)
     * @param title Notification title
     * @param message Success message
     */
    public static void notifySuccess(String missionUid, String title,
            String message) {
        notify(missionUid, NotificationUtil.GeneralIcon.SYNC_SUCCESS.getID(),
                title, message, true);
    }

    public static void notifySuccess(Feed feed, String title, String message) {
        notifySuccess(feed.uid, title, message);
    }

    public static void notifySuccess(Feed feed, String title) {
        notifySuccess(feed, title, title);
    }

    /**
     * Add recent activity to the list
     * @param missionUid Mission UID (null if general activity)
     * @param ra Recent activity object
     */
    public static void addRecentActivity(String missionUid, RecentActivity ra) {
        List<RecentActivity> raList = _raLists.get(missionUid);
        if (raList == null)
            raList = new ArrayList<RecentActivity>();
        raList.add(ra);
        _raLists.put(missionUid, raList);
    }

    /**
     * Check if a mission has logged activity
     * @param missionUid Mission UID (null for general activity)
     * @return True if recent activity has been logged
     */
    public static boolean hasRecentActivity(String missionUid) {
        return _raLists.containsKey(missionUid);
    }

    /**
     * Get the list of recent activity by mission UID
     * @param missionUid Mission UID (null for general activity)
     * @return List of recent activity objects
     */
    public static List<RecentActivity> getRecentActivity(String missionUid) {
        return new ArrayList<RecentActivity>(_raLists.get(missionUid));
    }

    /**
     * Set the intent provider this class will use to create notification intents
     * @param provider Intent provider
     */
    public static void setIntentProvider(IntentProvider provider) {
        _intentProvider = provider;
    }

    /**
     * Get notification intent
     * @param uid Feed UID
     * @return Intent
     */
    private static Intent getNotificationIntent(String uid) {
        return _intentProvider != null ? _intentProvider.createIntent(uid) : null;
    }

    /**
     * To be called when Data Sync is destroyed
     */
    public static void dispose() {
        // Clear notifications
        for (String k : _raLists.keySet()) {
            int nid = FeedUtils.getFeedNotificationId(k);
            NotificationUtil.getInstance().clearNotification(nid);
        }
        _raLists.clear();
    }
}
