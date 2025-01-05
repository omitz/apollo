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

package com.atakmap.android.missionapi.data;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.user.UserRole;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.comms.CotService;
import com.atakmap.comms.TAKServer;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.net.AtakAuthenticationCredentials;

import org.json.JSONObject;

/**
 * Authentication manager for feed passwords and access tokens
 */
public class AuthManager {

    private static final String TAG = "AuthManager";

    // Password used to join the feed (one-time use on subscribe)
    private static final String TYPE_PASSWORD = "DATA_SYNC_PASSWORD";

    // Token used to join a feed the user has been invited to (one-time use on subscribe)
    private static final String TYPE_INVITE_TOKEN = "DATA_SYNC_INVITE_TOKEN";

    // User's primary access token - This is their key to the feed after initial join
    // Paired with the user's role in the feed
    // Do NOT clear this token until the user unsubscribes
    private static final String TYPE_ACCESS_TOKEN = "DATA_SYNC_ACCESS_TOKEN";

    // Synchronization lock
    private static final Object lock = new Object();

    /**
     * Save password for a feed
     * @param feed Mission manifest
     * @param password Password
     */
    public static void savePassword(Feed feed, String password) {
        savePassword(feed.uid, feed.name, password);
    }

    public static void savePassword(TAKServer server, String feedName,
            String password) {
        String uid = FeedUtils.createUid(server.getConnectString(), feedName);
        savePassword(uid, feedName, password);
    }

    public static void savePassword(String feedUID, String feedName,
                                    String password) {
        synchronized (lock) {
            removeInviteToken(feedUID);
            CotService.getAuthenticationDatabase().saveCredentialsForType(
                    TYPE_PASSWORD, feedUID, feedName, password, true);
        }
    }

    /**
     * Get the password for an associated feed
     * @param feed Mission manifest
     * @return Password
     */
    public static String getPassword(Feed feed) {
        return getPassword(feed.uid);
    }

    public static String getPassword(String feedUID) {
        AtakAuthenticationCredentials creds = CotService
                .getAuthenticationDatabase().getCredentialsForType(
                        TYPE_PASSWORD, feedUID);
        return creds != null ? creds.password : null;
    }

    public static void removePassword(String feedUid) {
        synchronized (lock) {
            CotService.getAuthenticationDatabase().invalidateForType(TYPE_PASSWORD,
                    feedUid);
        }
    }

    public static void removePassword(Feed feed) {
        removePassword(feed.uid);
    }

    /**
     * Save the invite token for this feed
     * This token functions as a password used for subscribing to a feed
     * via invite by another user
     * @param feed Mission manifest
     * @param token Invite token
     */
    public static void saveInviteToken(Feed feed, String token) {
        synchronized (lock) {
            removePassword(feed);
            CotService.getAuthenticationDatabase().saveCredentialsForType(
                    TYPE_INVITE_TOKEN, feed.uid, feed.name,
                    token, true);
        }
    }

    public static String getInviteToken(Feed feed) {
        AtakAuthenticationCredentials creds = CotService
                .getAuthenticationDatabase().getCredentialsForType(
                        TYPE_INVITE_TOKEN, feed.uid);
        return creds != null ? creds.password : null;
    }

    public static boolean hasInviteToken(Feed feed) {
        return !FileSystemUtils.isEmpty(getInviteToken(feed));
    }

    public static void removeInviteToken(Feed feed) {
        removeInviteToken(feed.uid);
    }

    public static void removeInviteToken(String feedUid) {
        synchronized (lock) {
            CotService.getAuthenticationDatabase().invalidateForType(
                    TYPE_INVITE_TOKEN, feedUid);
        }
    }

    /**
     * Save access token and user role for a feed
     * The token acts as a perfeed key for all feed-specific operations
     * @param feed Mission manifest
     * @param role User role
     * @param token Token string
     */
    public static void saveAccessToken(Feed feed, UserRole role,
            String token) {
        synchronized (lock) {
            removePassword(feed);
            removeInviteToken(feed);
            if (FileSystemUtils.isEmpty(token)) {
                removeAccessToken(feed);
                return;
            }
            if (role == null)
                role = UserRole.SUBSCRIBER;
            CotService.getAuthenticationDatabase().saveCredentialsForType(
                    TYPE_ACCESS_TOKEN, feed.uid, role.name(), token, true);
            Log.d(TAG, "Op: Saved access credentials for " + feed.uid
                    + " w/ role " + role.name());
        }
    }

    public static void saveAccessToken(Feed feed,
            JSONObject data) {
        try {
            String token = null;
            UserRole role = UserRole.SUBSCRIBER;
            if (data.has("token"))
                token = data.getString("token");
            if (data.has("role"))
                role = UserRole.fromJSON(data.getJSONObject("role"));
            saveAccessToken(feed, role, token);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save auth token for " + feed.uid, e);
        }
    }

    /**
     * Get access token for a feed
     * @param feed Mission manifest
     * @return Token string
     */
    public static String getAccessToken(Feed feed) {
        return getAccessToken(feed.uid);
    }

    public static String getAccessToken(String feedUid) {
        AtakAuthenticationCredentials creds = getAccessCreds(feedUid);
        return creds != null ? creds.password : null;
    }

    public static boolean hasAccessToken(Feed feed) {
        return !FileSystemUtils.isEmpty(getAccessToken(feed));
    }

    /**
     * Set the role for the current access token (if there is one)
     * @param feed Mission manifest
     * @param role Role
     * @return True if role changed
     */
    public static boolean saveRole(Feed feed, UserRole role) {
        synchronized (lock) {
            AtakAuthenticationCredentials creds = getAccessCreds(feed.uid);
            if (creds == null || FileSystemUtils.isEmpty(creds.password)
                    || FileSystemUtils.isEquals(role.name(), creds.username))
                return false;
            saveAccessToken(feed, role, creds.password);
            return true;
        }
    }

    /**
     * Get the local user's role in this feed
     * @param feed Mission manifest
     * @return User role ('SUBSCRIBER' by default)
     */
    public static UserRole getRole(Feed feed) {
        AtakAuthenticationCredentials creds = getAccessCreds(feed.uid);
        return UserRole.valueOf(creds);
    }

    private static AtakAuthenticationCredentials getAccessCreds(
            String feedUid) {
        return CotService.getAuthenticationDatabase().getCredentialsForType(
                TYPE_ACCESS_TOKEN, feedUid);
    }

    /**
     * Remove access token and user role from the auth database
     * @param feedUid Mission UID
     */
    public static void removeAccessToken(String feedUid) {
        synchronized (lock) {
            AtakAuthenticationCredentials creds = getAccessCreds(feedUid);
            CotService.getAuthenticationDatabase().invalidateForType(
                    TYPE_ACCESS_TOKEN, feedUid);
            if (creds != null && creds.type.equals(TYPE_ACCESS_TOKEN))
                Log.d(TAG, "Op: Removed access credentials for " + feedUid);
        }
    }

    public static void removeAccessToken(Feed feed) {
        removeAccessToken(feed.uid);
    }

    /**
     * Clear all authentication related to a feed
     * Should only be called on un-subscribe
     * @param feedUid Mission UID
     */
    public static void clearAuth(String feedUid) {
        removeAccessToken(feedUid);
        removeInviteToken(feedUid);
        removePassword(feedUid);
    }
}
