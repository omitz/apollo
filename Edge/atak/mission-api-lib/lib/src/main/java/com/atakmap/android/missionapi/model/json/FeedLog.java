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

import android.graphics.Color;
import android.graphics.drawable.Drawable;

import com.atakmap.android.missionapi.model.xml.XMLUserLog;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.android.missionapi.util.TimeUtils;
import com.atakmap.android.missionapi.view.AttachmentDrawable;
import com.atakmap.android.maps.MapItem;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Feed log entry
 */
public class FeedLog extends FeedContent implements JSONSerializable {

    private static final String TAG = "FeedLog";

    public String uid, entryUID, serverTime, content;
    public Set<String> entryHashes = new HashSet<>();

    protected FeedLog(Feed feed) {
        super(feed);
    }

    protected FeedLog(Feed feed, JSONData data) {
        super(feed, data);
        this.uid = data.get("manifestUid", (String) data.get("id"));
        this.entryUID = data.get("resourceUid", (String) data.get("entryUid"));
        this.serverTime = data.get("servertime");
        this.content = data.get("log", (String) data.get("content"));
        this.creatorUID = data.get("creatorUid");
        this.entryHashes.addAll(data.getList("contentHashes", ""));
        setHashtags(data.getList("keywords", ""));
        setDTG((String) data.get("dtg"));
    }

    protected FeedLog(FeedLog other) {
        super(other);
        this.uid = other.getUID();
        this.entryUID = other.entryUID;
        this.serverTime = other.serverTime;
        this.content = other.content;
        this.entryHashes.addAll(other.entryHashes);
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = new JSONData(server);
        data.set("id", this.uid);
        data.set("entryUid", this.entryUID);
        data.set("dtg", this.timestamp);
        data.set("creatorUid", this.creatorUID);
        data.set("content", this.content);
        if (!server)
            data.set("servertime", this.serverTime);
        data.set("contentHashes", this.entryHashes);
        data.set("keywords", getHashtags());
        return data;
    }

    @Override
    public void update(FeedEntry entry, boolean fromServer) {
        super.update(entry, fromServer);
        if (entry instanceof FeedLog) {
            FeedLog log = (FeedLog) entry;
            this.uid = log.uid;
            this.entryUID = log.entryUID;
            this.serverTime = log.serverTime;
            this.content = log.content;
            this.entryHashes.clear();
            this.entryHashes.addAll(log.entryHashes);
        }
    }

    private void setDTG(String dtg) {
        // DTG used to be the UNIX epoch time in milliseconds
        // But at some point was changed to a timestamp
        // So we need to account for both
        Date d = TimeUtils.parseTimestamp(dtg);
        if (d == null) {
            try {
                dtg = TimeUtils.getTimestamp(Long.parseLong(dtg));
            } catch (Exception e) {
                Log.e(TAG, "Invalid DTG: " + dtg);
                dtg = "";
            }
        }
        this.timestamp = dtg;
        this.time = TimeUtils.parseTimestampMillis(dtg);
    }

    @Override
    public String getTitle() {
        return content;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String getUID() {
        return uid;
    }

    @Override
    public String getURI() {
        return "user-log://" + getUID();
    }

    @Override
    public String getGroupType() {
        return "user-log";
    }

    @Override
    public Drawable getIconDrawable() {
        Drawable icon = IconUtils.getDrawable(R.drawable.ic_details);
        Drawable attIcon = null;
        int color = Color.WHITE;
        MapItem item = FeedUtils.findMapItem(getEntryUID());
        if (item != null) {
            attIcon = item.getIconDrawable();
            color = item.getIconColor();
        }
        if (attIcon != null) {
            AttachmentDrawable dr = new AttachmentDrawable(icon, attIcon);
            dr.setAttachmentColor(color);
            return dr;
        }
        return icon;
    }

    @Override
    public boolean removeLocalContent() {
        return false;
    }

    @Override
    public boolean isStoredLocally() {
        return true;
    }

    public String getEntryUID() {
        return this.entryUID;
    }

    public List<String> getEntryHashes() {
        return new ArrayList<>(this.entryHashes);
    }

    @Override
    public boolean isUnread() {
        return this.unread;
    }

    @Override
    public boolean setUnread(boolean unread) {
        if (this.unread != unread) {
            this.unread = unread;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return this.content + " (item: " + this.entryUID
                + ", files: " + this.entryHashes.size() + ")";
    }

    protected FeedLog(Feed feed, XMLUserLog log) {
        super(feed, log);

        this.uid = log.uid;
        this.entryUID = log.resourceUid;
        this.creatorUID = log.creatorUid;
        this.content = log.log;
        this.serverTime = log.servertime;
        this.entryHashes.addAll(log.contentHashes);
        setHashtags(log.keywords);
        setDTG(log.dtg);
    }
}
