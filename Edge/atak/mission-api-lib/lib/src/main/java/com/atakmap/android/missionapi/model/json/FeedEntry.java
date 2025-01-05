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

import com.atakmap.android.missionapi.data.AvailabilityState;
import com.atakmap.android.missionapi.interfaces.Timestamp;
import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.android.missionapi.util.TimeUtils;
import com.atakmap.android.hierarchy.action.GoTo;
import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.Comparator;
import java.util.Objects;

/**
 * Generic feed entry (content or change)
 */
public abstract class FeedEntry implements JSONSerializable, Timestamp, GoTo {

    public Feed feed;
    public String creatorUID;
    protected String timestamp;
    protected long time;
    protected boolean unread;

    protected FeedEntry(Feed feed) {
        this.feed = feed;
    }

    protected FeedEntry(Feed feed, JSONData data) {
        this(feed);
        this.creatorUID = data.get("creatorUid");
        setTimestamp((String) data.get("timestamp"));
        setUnread(data.get("unread", false));
    }

    protected FeedEntry(FeedEntry other) {
        this(other.feed);
        this.creatorUID = other.getCreatorUID();
        setTimestamp(other.getTimestamp());
        setUnread(other.unread);
    }

    /**
     * Update feed entry based on another
     * @param other Feed entry
     * @param fromServer True if the content is coming from the server
     */
    public void update(FeedEntry other, boolean fromServer) {
        this.creatorUID = other.getCreatorUID();
        setTimestamp(other.getTimestamp());
        if (!fromServer)
            setUnread(other.unread);
    }

    public void update(FeedEntry other) {
        update(other, true);
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = new JSONData(server);
        data.set("creatorUid", getCreatorUID());
        data.set("timestamp", getTimestamp());
        if (unread && !server)
            data.set("unread", unread);
        return data;
    }

    /**
     * Check if the entry is valid
     * @return Valid
     */
    public boolean isValid() {
        return true;
    }

    /**
     * Unique identifier for entry
     * @return UID
     */
    public abstract String getUID();

    /**
     * Entry title
     * @return Name of this entry
     */
    public abstract String getTitle();

    /**
     * Group type used for visual categorization
     * @return Type describing this content
     */
    public abstract String getGroupType();

    /**
     * Get availability state
     * @return Availability state
     */
    public AvailabilityState getAvailability() {
        return isUnread() ? AvailabilityState.NEW : AvailabilityState.OK;
    }

    /**
     * Set timestamp
     * @param timestamp Timestamp string
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        this.time = TimeUtils.parseTimestampMillis(timestamp);
    }

    public void setTimestamp(long time) {
        this.time = time;
        this.timestamp = TimeUtils.getTimestamp(time);
    }

    public void setTime(long time) {
        setTimestamp(time);
    }

    /**
     * Get timestamp string
     * @return Timestamp
     */
    @Override
    public String getTimestamp() {
        return this.timestamp;
    }

    /**
     * Get time milliseconds
     * @return Time
     */
    @Override
    public long getTime() {
        return this.time;
    }

    /**
     * Get creator UID
     * @return Creator UID
     */
    public String getCreatorUID() {
        return this.creatorUID;
    }

    /**
     * Get creator name
     * @return Creator name
     */
    public String getCreatorName() {
        String name = FeedUtils.getUserCallsign(getCreatorUID(), feed);
        return FileSystemUtils.isEmpty(name) ? "User" : name;
    }

    /**
     * Check if this entry is self made
     * @return True if the creator UID is equal to device UID
     */
    public boolean isSelfMade() {
        return FileSystemUtils.isEquals(getCreatorUID(),
                MapView.getDeviceUid());
    }

    /**
     * Get icon drawable
     * @return Icon drawable
     */
    public Drawable getIconDrawable() {
        return null;
    }

    /**
     * Get icon color
     * @return Color int
     */
    public int getIconColor() {
        return Color.WHITE;
    }

    /**
     * Is content unread
     * @return True if unread
     */
    public boolean isUnread() {
        return this.unread;
    }

    /**
     * Set unread flag, return true if value was changed
     * @param unread Unread flag
     * @return True if changed, false if not
     */
    public boolean setUnread(boolean unread) {
        if (this.unread != unread) {
            this.unread = unread;
            return true;
        }
        return false;
    }

    @Override
    public boolean goTo(boolean select) {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeedEntry feedEntry = (FeedEntry) o;
        return time == feedEntry.time &&
                Objects.equals(creatorUID, feedEntry.creatorUID) &&
                Objects.equals(timestamp, feedEntry.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creatorUID, timestamp, time, unread);
    }

    /**
     * Return true if the content matches the specified search terms
     *
     * @param terms Terms to search
     * @return True if match
     */
    public boolean search(String terms) {
        if (!isValid())
            return false;

        // Title
        if (StringUtils.find(getTitle(), terms))
            return true;

        // Creator callsign
        String callsign = FeedUtils.getUserCallsign(getCreatorUID(), null);
        if (StringUtils.find(callsign, terms))
            return true;

        return false;
    }

    /**
     * Download content
     * @return True if downloaded started
     */
    public boolean download() {
        return false;
    }

    public static final Comparator<FeedEntry> SORT_TITLE = new Comparator<FeedEntry>() {
        @Override
        public int compare(FeedEntry e1, FeedEntry e2) {
            String t1 = e1.getTitle(), t2 = e2.getTitle();
            if (t1 == null) t1 = "";
            if (t2 == null) t2 = "";
            return t1.compareTo(t2);
        }
    };

    public static final Comparator<FeedEntry> SORT_TIME = new Comparator<FeedEntry>() {
        @Override
        public int compare(FeedEntry e1, FeedEntry e2) {
            int tc = Long.compare(e2.getTime(), e1.getTime());
            if (tc != 0)
                return tc;
            return SORT_TITLE.compare(e1, e2);
        }
    };
}
