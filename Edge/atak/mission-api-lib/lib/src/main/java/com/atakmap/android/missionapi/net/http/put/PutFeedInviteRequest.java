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

package com.atakmap.android.missionapi.net.http.put;

import com.atakmap.android.missionapi.data.TAKServerVersion;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.user.UserRole;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.OpType;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.URIPathBuilder;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Invite an online or offline user to a feed via TAK server using
 * their UID or callsign
 */
public class PutFeedInviteRequest extends AbstractRequest {

    private static final String TAG = "PutFeedInviteRequest";

    public static class Invitation {
        public final String uid, callsign;
        public final UserRole role;

        Invitation(String uid, String callsign, UserRole role) {
            this.uid = uid;
            this.callsign = callsign;
            this.role = role;
        }
    }

    private final List<Invitation> _invitations = new ArrayList<>();

    public PutFeedInviteRequest(Feed feed) {
        super(feed);
    }

    public PutFeedInviteRequest(Feed feed, String userUID,
                                String callsign, UserRole role) {
        this(feed);
        addInvitation(userUID, callsign, role);
    }

    public PutFeedInviteRequest(Feed feed, String callsign,
                                UserRole role) {
        this(feed, null, callsign, role);
    }

    public PutFeedInviteRequest(Feed feed, String userUID,
                                String callsign) {
        this(feed, userUID, callsign, null);
    }

    public PutFeedInviteRequest(Feed feed, String callsign) {
        this(feed, null, callsign, null);
    }

    public PutFeedInviteRequest(JSONObject json) throws JSONException {
        super(json);
        JSONArray arr = json.getJSONArray("invitations");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String uid = null, callsign = null;
            UserRole role = null;
            if (o.has("uid"))
                uid = o.getString("uid");
            if (o.has("callsign"))
                callsign = o.getString("callsign");
            if (o.has("role"))
                role = UserRole.fromString(o.getString("role"));
            addInvitation(uid, callsign, role);
        }
    }

    public void addInvitation(String userUID, String callsign, UserRole role) {
        _invitations.add(new Invitation(userUID, callsign, role));
    }

    public List<Invitation> getInvitations() {
        return new ArrayList<>(_invitations);
    }

    public OpType getOperationType() {
        return checkSupport(TAKServerVersion.SUPPORT_BULK_INVITE)
                ? OpType.POST : OpType.PUT;
    }

    @Override
    public int getRequestType() {
        return RestManager.PUT_FEED_INVITE;
    }

    @Override
    public String getTag() {
        return TAG;
    }

    @Override
    public String getRequestEndpoint(String... args) {
        URIPathBuilder sb = new URIPathBuilder(super.getRequestEndpoint());
        sb.addPath("invite");
        if (!checkSupport(TAKServerVersion.SUPPORT_BULK_INVITE)) {
            if (!FileSystemUtils.isEmpty(args[0]))
                sb.addPath("clientUid").addPath(args[0]);
            else
                sb.addPath("callsign").addPath(args[1]);
            UserRole role = UserRole.fromString(args[2]);
            if (role != null && checkSupport(TAKServerVersion.SUPPORT_ROLES))
                sb.addParam("role", role.toServerString());
        }
        sb.addParam("creatorUid", getCreatorUID());
        return sb.toString();
    }

    @Override
    public String getRequestBody() {
        if (!checkSupport(TAKServerVersion.SUPPORT_BULK_INVITE))
            return null;
        JSONArray arr = new JSONArray();
        try {
            for (Invitation inv : _invitations) {
                JSONObject o = new JSONObject();
                if (!FileSystemUtils.isEmpty(inv.uid)) {
                    o.put("type", "clientUid");
                    o.put("invitee", inv.uid);
                } else {
                    o.put("type", "callsign");
                    o.put("invitee", inv.callsign);
                }
                if (inv.role != null) {
                    JSONObject rType = new JSONObject();
                    rType.put("type", inv.role.toServerString());
                    o.put("role", rType);
                }
                arr.put(o);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get request body", e);
        }
        return arr.toString();
    }

    @Override
    public String getErrorMessage() {
        return StringUtils.getString(R.string.mn_failed_to_invite_users,
                getFeedName());
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            JSONArray arr = new JSONArray();
            for (Invitation inv : _invitations) {
                JSONObject o = new JSONObject();
                if (inv.uid != null)
                    o.put("uid", inv.uid);
                if (inv.callsign != null)
                    o.put("callsign", inv.callsign);
                if (inv.role != null)
                    o.put("role", inv.role);
                arr.put(o);
            }
            json.put("invitations", arr);
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }
}
