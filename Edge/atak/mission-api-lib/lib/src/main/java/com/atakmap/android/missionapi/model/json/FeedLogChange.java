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

import com.atakmap.android.missionapi.model.xml.XMLMissionChangeData;

/**
 * Feed change for user logs
 */
public class FeedLogChange extends FeedChange {

    public FeedLog log;

    public FeedLogChange(Feed feed, JSONData data) {
        super(feed, data);
        this.log = Feed.createLog(feed, data.getChild("logEntry"));
    }

    public FeedLogChange(FeedLog log) {
        super(log.feed, log);
        this.log = Feed.createLog(log);
    }

    @Override
    public JSONData toJSON(boolean server) {
        JSONData data = super.toJSON(server);
        data.set("logEntry", this.log);
        return data;
    }

    @Override
    public String getTitle() {
        return this.log.content;
    }

    @Override
    public String getContentUID() {
        return this.log.uid;
    }

    @Override
    public String getContentTitle() {
        return this.log.content;
    }

    @Override
    public FeedContent getContent() {
        return this.log;
    }

    public FeedLogChange(Feed feed, XMLMissionChangeData xml) {
        super(feed, xml);
        this.log = Feed.createLog(feed, xml.logEntry);
    }
}
