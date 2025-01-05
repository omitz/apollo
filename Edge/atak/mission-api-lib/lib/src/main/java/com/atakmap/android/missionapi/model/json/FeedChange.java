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

import android.graphics.drawable.Drawable;

import com.atakmap.android.missionapi.model.xml.XMLMissionChangeData;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.android.missionapi.util.TimeUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.util.Comparator;

/**
 * Change entry in a feed
 */
public class FeedChange extends FeedEntry {

    public static final String MISSION_CHANGE_MATCHER = "MissionChanges";

    public static final Comparator<FeedChange> TIME_SORT = new Comparator<FeedChange>() {
        @Override
        public int compare(FeedChange lhs, FeedChange rhs) {
            return Long.compare(rhs.getTime(), lhs.getTime());
        }
    };

    public enum Type {
        // Mission created or deleted (should only be used once)
        CREATE_MISSION(R.string.create_mission),
        DELETE_MISSION(R.string.delete_mission),
        // Content added/updated or deleted
        ADD_CONTENT(R.string.add_content),
        REMOVE_CONTENT(R.string.remove_content);

        public final String name;

        Type(int name) {
            this.name = StringUtils.getString(name);
        }

        static Type fromValue(String name) {
            try {
                return Type.valueOf(name);
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    public Type type;
    public String serverTimestamp, feedName;
    public long serverTime;

    public static FeedChange fromJSON(Feed feed, JSONData data) {
        if (data.has("details"))
            return new FeedCoTChange(feed, data);
        else if (data.has("contentResource"))
            return new FeedFileChange(feed, data);
        else if (data.has("externalData"))
            return new FeedExternalChange(feed, data);
        else if (data.has("logEntry"))
            return new FeedLogChange(feed, data);

        return new FeedChange(feed, data);
    }

    protected FeedChange(Feed feed) {
        super(feed);
    }

    protected FeedChange(Feed feed, JSONData data) {
        super(feed, data);
        this.type = Type.fromValue((String) data.get("type"));
        this.time = TimeUtils.parseTimestampMillis(this.timestamp);
        setServerTime(data.get("serverTime", ""));
        this.feedName = data.get("missionName");

    }

    protected FeedChange(Feed feed, FeedContent content) {
        super(feed);
        this.feedName = feed.name;
        this.creatorUID = content.getCreatorUID();
        setTimestamp(new CoordinatedTime().getMilliseconds());
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("type", this.type);
        if (!server && !FileSystemUtils.isEmpty(this.serverTimestamp))
            data.set("serverTime", this.serverTimestamp);
        data.set("missionName", this.feedName);
        return data;
    }

    @Override
    public boolean isValid() {
        return type != null && !FileSystemUtils.isEmpty(timestamp)
                && !FileSystemUtils.isEmpty(feedName);
    }

    @Override
    public String getUID() {
        StringBuilder sb = new StringBuilder();
        sb.append(getCreatorUID()).append("/");
        sb.append(getType().name()).append("/");
        sb.append(getTimestamp());
        String contentUID = getContentUID();
        if (contentUID != null)
            sb.append("/").append(contentUID);
        return sb.toString();
    }

    @Override
    public String getTitle() {
        StringBuilder title = new StringBuilder();
        if (isValid()) {
            title.append(getCreatorName());
            title.append(" ");
            int msgId;
            switch (type) {
                default:
                case ADD_CONTENT:
                    msgId = feed.changes.isFirstAdd(this)
                            ? R.string.user_added_content
                            : R.string.user_updated_content;
                    break;
                case REMOVE_CONTENT:
                    msgId = R.string.user_removed_content;
                    break;
                case CREATE_MISSION:
                    msgId = R.string.user_created_mission;
                    break;
                case DELETE_MISSION:
                    msgId = R.string.user_deleted_mission;
                    break;
            }
            title.append(StringUtils.getString(msgId)).append(" ");
            title.append(getContentTitle());
        } else
            title.append(StringUtils.getString(R.string.invalid_change));
        return title.toString();
    }

    @Override
    public String getGroupType() {
        return "change-log";
    }

    @Override
    public Drawable getIconDrawable() {
        if (type == Type.CREATE_MISSION)
            return IconUtils.getDrawable(R.drawable.ic_add);
        else if (type == Type.DELETE_MISSION)
            return IconUtils.getDrawable(R.drawable.ic_delete);
        return IconUtils.getDrawable(R.drawable.ic_unknown);
    }

    /**
     * Get the title of this content associated with this change
     * @return Content title
     */
    public String getContentTitle() {
        if (type == Type.CREATE_MISSION || type == Type.DELETE_MISSION)
            return feedName;
        String title = "";
        if (this.feed != null) {
            FeedContent content = getContent();
            if (content != null)
                title = content.getTitle();
        }
        if (FileSystemUtils.isEmpty(title))
            title = StringUtils.getString(R.string.content_untitled);
        return title;
    }

    /**
     * Get the associated content's UID (if there is one)
     * @return Content UID or null if N/A
     */
    public String getContentUID() {
        return null;
    }

    /**
     * Get the content associated with this change
     * @return Content
     */
    public FeedContent getContent() {
        return feed != null ? feed.getContent(getContentUID()) : null;
    }

    /**
     * Get the change type
     * @return Change type
     */
    public Type getType() {
        return this.type;
    }

    /**
     * Get feed associated with this change
     * @return Feed
     */
    public Feed getFeed() {
        return feed;
    }

    /**
     * Get feed name
     * @return Feed name
     */
    public String getFeedName() {
        return this.feedName;
    }

    /**
     * Set server-side timestamp
     * @param ts Timestamp
     */
    public void setServerTime(String ts) {
        this.serverTimestamp = ts;
        this.serverTime = TimeUtils.parseTimestampMillis(ts);
    }

    public void setServerTime(long time) {
        this.serverTime = time;
        this.serverTimestamp = TimeUtils.getTimestamp(time);
    }

    @Override
    public String toString() {
        return getTitle() + " (" + getContentUID() + ")";
    }

    @Override
    public boolean goTo(boolean select) {
        FeedContent content = getContent();
        return content != null && content.goTo(select);
    }

    @Override
    public boolean search(String terms) {
        if (super.search(terms))
            return true;

        FeedContent content = getContent();
        return content != null && content.search(terms);
    }

    protected FeedChange(Feed feed, XMLMissionChangeData xml) {
        super(feed);
        setTimestamp(xml.timestamp);
        setServerTime(xml.serverTime);
        this.feedName = xml.missionName;
        this.creatorUID = xml.creatorUid;
        this.type = xml.type;
    }

    public static FeedChange fromXML(Feed feed, XMLMissionChangeData xml) {
        if (xml.details != null)
            return new FeedCoTChange(feed, xml);
        else if (xml.contentResource != null)
            return new FeedFileChange(feed, xml);
        else if (xml.externalData != null)
            return new FeedExternalChange(feed, xml);
        else if (xml.logEntry != null)
            return new FeedLogChange(feed, xml);

        return new FeedChange(feed, xml);
    }
}
