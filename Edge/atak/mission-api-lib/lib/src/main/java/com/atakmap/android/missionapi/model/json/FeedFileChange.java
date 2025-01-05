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

/**
 * Feed change for files
 */
public class FeedFileChange extends FeedChange {

    public FeedFileDetails details;

    public FeedFileChange(Feed feed) {
        super(feed);
    }

    public FeedFileChange(Feed feed, JSONData data) {
        super(feed, data);
        this.details = new FeedFileDetails(data.getChild("contentResource"));

    }

    public FeedFileChange(Feed feed, FeedFile ff) {
        super(feed, ff);
        this.details = ff.getDetails();
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("contentResource", this.details);
        return data;
    }

    @Override
    public String getContentUID() {
        return this.details.hash;
    }

    @Override
    public String getContentTitle() {
        return this.details.name;
    }

    @Override
    public FeedContent getContent() {
        FeedContent content = super.getContent();
        if (content != null)
            return content;

        // Generate content based on metadata
        return Feed.createFile(this);
    }

    @Override
    public Drawable getIconDrawable() {
        return this.details.getIconDrawable();
    }

    public FeedFileChange(Feed feed, XMLMissionChangeData xml) {
        super(feed, xml);
        this.details = new FeedFileDetails(xml.contentResource);
    }
}
