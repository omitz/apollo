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

import com.atakmap.android.missionapi.data.AvailabilityState;
import com.atakmap.android.missionapi.model.xml.XMLMissionContent;
import com.atakmap.android.hashtags.util.HashtagSet;
import com.atakmap.android.hashtags.util.HashtagUtils;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Abstract feed content (map item, file, log)
 */
public abstract class FeedContent extends FeedEntry {

    protected final HashtagSet hashtags = new HashtagSet();

    protected boolean desynced;

    protected FeedContent(Feed feed) {
        super(feed);
    }

    protected FeedContent(Feed feed, JSONData data) {
        super(feed, data);
        setHashtags(data.getList("keywords", ""));
    }

    protected FeedContent(FeedContent other) {
        super(other);
        setHashtags(other.getHashtags());
    }

    protected FeedContent(FeedChange change) {
        super(change);
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("keywords", getHashtags());
        return data;
    }

    /**
     * Update feed content based on other content instance (usually of the same type)
     * @param entry Other content
     * @param fromServer From server
     */
    @Override
    public void update(FeedEntry entry, boolean fromServer) {
        super.update(entry, fromServer);
        if (entry instanceof FeedContent) {
            FeedContent content = (FeedContent) entry;
            setHashtags(content.getHashtags());
        }
    }

    /**
     * Get a URI representing this content
     * @return Content URI
     */
    public abstract String getURI();

    /**
     * Remove local content from the device (but not from the server)
     * i.e. remove the item from the map
     * @return True if removed, false if not
     */
    public abstract boolean removeLocalContent();

    /**
     * List of hashtags associated with this mission content
     * @return Keywords list
     */
    public Collection<String> getHashtags() {
        synchronized (this.hashtags) {
            return this.hashtags;
        }
    }

    /**
     * Get # separated list of tags (hashtags)
     * @return Hashtags string
     */
    public String getHashtagsString() {
        return StringUtils.getHashtagsString(getHashtags());
    }

    /**
     * Set hashtags list
     * @param tags Keywords list
     */
    public void setHashtags(Collection<String> tags) {
        HashtagSet added, removed;
        synchronized (this.hashtags) {
            added = new HashtagSet();
            removed = new HashtagSet(this.hashtags);
            this.hashtags.clear();
            for (String tag : tags) {
                tag = HashtagUtils.validateTag(tag);
                if (!FileSystemUtils.isEmpty(tag)) {
                    this.hashtags.add(tag);
                    added.add(tag);
                }
            }
            removed.removeAll(this.hashtags);
        }
        HashtagSet local = getLocalHashtags();
        local.addAll(added);
        local.removeAll(removed);
        setLocalHashtags(local);
    }

    /**
     *
     * @return
     */
    public HashtagSet getLocalHashtags() {
        return new HashtagSet();
    }

    /**
     * Update the hashtags on the local content
     */
    public void setLocalHashtags(HashtagSet tags) {
    }

    /**
     * Check if the content keys are the same
     * i.e. same class, same UID, same hash
     * Should be less strict than equals()
     * @return True if content key is equivalent
     */
    public boolean contentEquals(FeedContent o) {
        return Objects.equals(getUID(), o.getUID());
    }

    public boolean isStoredLocally() {
        return false;
    }

    /**
     * Is content missing (usually only happens to map items)
     * @return True if missing
     */
    public boolean isMissing() {
        return false;
    }

    /**
     * Set content out of sync
     * @param desync True if out of sync
     */
    public void setDesynced(boolean desync) {
        this.desynced = desync;
    }

    /**
     * Content is out of sync with the server
     * @return True if out of sync
     */
    public boolean isDesynced() {
        return desynced;
    }

    @Override
    public boolean setUnread(boolean unread) {
        boolean ret = false;
        if (feed != null) {
            List<FeedChange> changes = feed.changes.getChanges(getUID());
            for (FeedChange change : changes)
                ret |= change.setUnread(false);
        }
        return ret;
    }

    @Override
    public boolean isUnread() {
        if (feed != null) {
            FeedChange change = feed.changes.getLatestChange(getUID());
            return change != null && change.isUnread();
        }
        return false;
    }

    @Override
    public AvailabilityState getAvailability() {
        // Content missing from server
        if (isMissing())
            return AvailabilityState.MISSING;

        // Content not downloaded
        if (!isStoredLocally())
            return AvailabilityState.AVAILABLE;

        // Pending offline request
        if (feed != null && !feed.isConnected() && hasOfflineUpdate())
            return AvailabilityState.OFFLINE;

        // Out-of-sync content
        if (isDesynced())
            return AvailabilityState.DESYNCED;

        return super.getAvailability();
    }

    /**
     * Get estimated disk size of this content
     * Note: Files are the only content type that are remotely accurate
     * @return Estimated size in bytes
     */
    public long getEstimatedFileSize() {
        // Arbitrary guess = 512 bytes
        return 512;
    }

    /**
     * Cache metadata for this content so we can check for changes against it later
     */
    public void cache() {
    }

    /**
     * Return true if the content matches the specified search terms
     *
     * @param terms Terms to search
     * @return True if match
     */
    public boolean search(String terms) {
        if (super.search(terms))
            return true;

        // Keywords
        Collection<String> hashtags = getHashtags();
        if (hashtags != null && !hashtags.isEmpty()) {
            for (String kw : hashtags) {
                if (StringUtils.find(kw, terms))
                    return true;
            }
        }

        return false;
    }

    /**
     * Check if this content has an offline update pending
     * @return True if update pending
     */
    public boolean hasOfflineUpdate() {
        return false;
    }

    @Override
    public String toString() {
        return getTitle() + ", " + getGroupType() + ", " + getUID();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        FeedContent that = (FeedContent) o;
        return super.equals(that)
                && contentEquals(that)
                && Objects.equals(getHashtags(), that.getHashtags());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getHashtags());
    }

    public FeedContent(Feed feed, XMLMissionContent mc) {
        super(feed);
        this.unread = mc.unread;
    }
}
