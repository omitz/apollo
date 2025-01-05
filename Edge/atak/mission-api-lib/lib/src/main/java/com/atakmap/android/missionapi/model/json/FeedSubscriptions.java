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

import com.atakmap.android.missionapi.model.json.user.UserRole;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data for user subscriptions to a feed
 */
public class FeedSubscriptions {

    private final Map<String, UserRole> userRoleMap = new HashMap<>();

    public FeedSubscriptions(JSONData data) {
        JSONData subs = data.getChild("data");
        for (JSONData d : subs) {
            String clientUid;
            UserRole role = UserRole.SUBSCRIBER;
            if (d.isObject()) {
                clientUid = d.get("clientUid");
                if (d.has("role"))
                    role = UserRole.fromJSON((JSONObject) d.get("role"));
            } else
                clientUid = d.asString();
            if (!FileSystemUtils.isEmpty(clientUid))
                userRoleMap.put(clientUid, role);
        }
    }

    public boolean isValid() {
        return true;
    }

    public List<String> getUids() {
        return new ArrayList<>(userRoleMap.keySet());
    }

    public UserRole getRole(String uid) {
        return userRoleMap.get(uid);
    }

    public boolean hasUids() {
        return !userRoleMap.isEmpty();
    }

    public int size() {
        return userRoleMap.size();
    }
}
