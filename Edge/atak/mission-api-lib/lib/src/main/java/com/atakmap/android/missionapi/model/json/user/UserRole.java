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

package com.atakmap.android.missionapi.model.json.user;

import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.math.MathUtils;
import com.atakmap.net.AtakAuthenticationCredentials;

import org.json.JSONObject;

/**
 * Role of a user in a mission
 */
public enum UserRole {

    READ_ONLY(R.string.role_read_only, R.string.role_read_only_desc,
            UserPermission.READ),
    SUBSCRIBER(R.string.role_subscriber, R.string.role_subscriber_desc,
            UserPermission.READ | UserPermission.WRITE),
    OWNER(R.string.role_owner, R.string.role_owner_desc, UserPermission.ALL);

    private final int _nameId, _descId;
    private final int _flags;

    UserRole(int nameId, int descId, int flags) {
        _nameId = nameId;
        _descId = descId;
        _flags = flags;
    }

    public boolean hasPermission(int permission) {
        return MathUtils.hasBits(_flags, permission);
    }

    public String getName() {
        return StringUtils.getString(_nameId);
    }

    public String getDescription(String username) {
        return StringUtils.getString(_descId, username);
    }

    public String toServerString() {
        if (this == UserRole.READ_ONLY)
            return "MISSION_READONLY_SUBSCRIBER";
        return "MISSION_" + name();
    }

    public static UserRole fromString(String name) {
        if (name == null)
            return UserRole.SUBSCRIBER;
        try {
            if (name.startsWith("MISSION_"))
                name = name.substring("MISSION_".length());
            if (name.equals("READONLY_SUBSCRIBER"))
                return UserRole.READ_ONLY;
            return UserRole.valueOf(name);
        } catch (Exception e) {
            return UserRole.SUBSCRIBER;
        }
    }

    public static UserRole valueOf(AtakAuthenticationCredentials creds) {
        return creds != null ? fromString(creds.username) : UserRole.SUBSCRIBER;
    }

    public static UserRole fromJSON(Object o) {
        if (o instanceof String)
            return fromString(String.valueOf(o));
        else if (o instanceof JSONObject)
            return fromJSON((JSONObject) o);
        else if (o instanceof JSONData)
            return fromJSON(((JSONData) o).asJSONObject());
        return UserRole.SUBSCRIBER;
    }

    public static UserRole fromJSON(JSONObject obj) {
        try {
            String role = obj.getString("type");
            //JSONArray permissions = obj.getJSONArray("permissions");
            return fromString(role);
        } catch (Exception e) {
            return null;
        }
    }

    public static UserRole fromCot(CotDetail d) {
        return fromString(d.getAttribute("type"));
    }
}
