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

import com.atakmap.android.data.URIContentHandler;
import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.missionapi.data.AvailabilityState;
import com.atakmap.android.missionapi.model.xml.XMLExternalData;
import com.atakmap.android.missionapi.util.IconUtils;
import com.atakmap.android.missionapi.util.StringUtils;

import java.util.Objects;

/**
 * External data link within a feed
 */
public class FeedExternalData extends FeedURIContent {

    public String uid, name, tool, urlData, urlView;

    protected FeedExternalData(Feed feed, FeedExternalDetails details) {
        super(feed);
        this.uid = details.uid;
        this.name = details.name;
        this.tool = details.tool;
        this.urlData = details.urlData;
        this.urlView = details.urlView;
    }

    protected FeedExternalData(Feed feed, JSONData data) {
        this(feed, new FeedExternalDetails(data));
        setHashtags(data.getList("keywords", ""));
    }

    protected FeedExternalData(FeedExternalChange change) {
        this(change.feed, change.details);
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("uid", this.uid);
        data.set("name", this.name);
        data.set("tool", this.tool);
        data.set("urlData", this.urlData);
        data.set("urlView", this.urlView);
        return data;
    }

    @Override
    public void update(FeedEntry entry, boolean fromServer) {
        super.update(entry, fromServer);
        if (entry instanceof FeedExternalData) {
            FeedExternalData data = (FeedExternalData) entry;
            this.uid = data.uid;
            this.name = data.name;
            this.tool = data.tool;
            this.urlData = data.urlData;
            this.urlView = data.urlView;
        }
    }

    @Override
    public String getTitle() {
        return this.name;
    }

    @Override
    public String getUID() {
        return this.uid;
    }

    @Override
    public String getURI() {
        return getTool() + "://" + getUID();
    }

    @Override
    public String getGroupType() {
        return "externaldata/" + getTool();
    }

    @Override
    public Drawable getIconDrawable() {
        return IconUtils.getDrawable(R.drawable.ic_external_data);
    }

    @Override
    public long getTime() {
        FeedExternalChange change = getChange();
        return change != null ? change.getTime() : 0L;
    }

    @Override
    public String getTimestamp() {
        FeedExternalChange change = getChange();
        return change != null ? change.getTimestamp() : "";
    }

    @Override
    public String getCreatorUID() {
        FeedExternalChange change = getChange();
        return change != null ? change.getCreatorUID() : "";
    }

    public String getTool() {
        return this.tool;
    }

    public String getDataURL() {
        return this.urlData;
    }

    public String getViewURL() {
        return this.urlView;
    }

    @Override
    public URIContentHandler getContentHandler() {
        return URIContentManager.getInstance().getHandler(this.tool, this.urlData);
    }

    @Override
    public boolean search(String terms) {
        if (!isValid())
            return false;

        if (StringUtils.find(this.name, terms)
                || StringUtils.find(this.tool, terms)
                || StringUtils.find(this.urlData, terms))
            return true;

        return super.search(terms);
    }

    @Override
    public boolean isStoredLocally() {
        // XXX - Don't want to display content as "available" if the user
        // doesn't plan on installing the associated tool
        //return getContentHandler() != null;
        return true;
    }

    @Override
    public AvailabilityState getAvailability() {
        if (getContentHandler() == null)
            return AvailabilityState.AVAILABLE;
        return super.getAvailability();
    }

    @Override
    public String toString() {
        return this.urlData != null ? this.urlData : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        FeedExternalData that = (FeedExternalData) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.uid, that.uid) &&
                Objects.equals(this.tool, that.tool) &&
                Objects.equals(this.urlView, that.urlView) &&
                Objects.equals(this.urlData, that.urlData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.name, this.uid, this.tool,
                this.urlView, this.urlData);
    }

    @Override
    public boolean contentEquals(FeedContent o) {
        if (o instanceof FeedExternalData) {
            FeedExternalData mc = (FeedExternalData) o;
            return Objects.equals(this.uid, mc.uid) &&
                    Objects.equals(this.name, mc.name) &&
                    Objects.equals(this.urlData, mc.urlData);
        }
        return false;
    }

    protected FeedExternalData(Feed feed, XMLExternalData data) {
        super(feed, data);

        this.uid = data.uid;
        this.name = data.name;
        this.tool = data.tool;
        this.urlView = data.urlView;
        this.urlData = data.urlData;
    }

    private FeedExternalChange getChange() {
        if (feed == null)
            return null;
        FeedChange change = feed.changes.getLatestChange(getUID());
        return change instanceof FeedExternalChange
                ? (FeedExternalChange) change : null;
    }
}
