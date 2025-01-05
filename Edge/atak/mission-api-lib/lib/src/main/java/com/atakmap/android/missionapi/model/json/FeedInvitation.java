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

package com.atakmap.android.missionapi.model.json;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.model.json.user.UserRole;
import com.atakmap.android.missionapi.util.TimeUtils;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Mission invitation struct (password and roles API)
 */
public class FeedInvitation {

    public final String serverNCS;
    public final String missionName;
    public String invitee;
    public String type;
    public String creatorUid;
    public long createTime;
    public String token;
    public UserRole role;

    public FeedInvitation(String serverNCS, JSONObject obj)
            throws JSONException {
        this.serverNCS = serverNCS;
        this.missionName = obj.getString("missionName");
        this.invitee = obj.getString("invitee");
        this.type = obj.getString("type");
        this.creatorUid = obj.getString("creatorUid");
        this.createTime = TimeUtils.parseTimestampMillis(
                obj.getString("createTime"));
        this.token = obj.getString("token");
        this.role = UserRole.fromJSON(obj.getJSONObject("role"));
    }

    public FeedInvitation(String serverNCS, CotDetail missionDetail) {
        this.serverNCS = serverNCS;
        this.missionName = missionDetail.getAttribute("name");
        this.creatorUid = missionDetail.getAttribute("authorUid");
        this.invitee = MapView.getDeviceUid();
        this.token = missionDetail.getAttribute("token");
        this.role = UserRole.fromCot(missionDetail.getFirstChildByName(
                0, "role"));
    }

    public FeedInvitation(String serverNCS, String missionName) {
        this.serverNCS = serverNCS;
        this.missionName = missionName;
        this.createTime = new CoordinatedTime().getMilliseconds();
        this.role = UserRole.SUBSCRIBER;
    }
}
