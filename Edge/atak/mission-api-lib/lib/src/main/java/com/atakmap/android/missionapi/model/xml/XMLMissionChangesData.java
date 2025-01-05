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

package com.atakmap.android.missionapi.model.xml;

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedChange;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by byoung on 8/25/17.
 */

public class XMLMissionChangesData {

    private static final String TAG = "XMLMissionChangesData";
    public static final String MISSION_CHANGE_MATCHER = "MissionChanges";

    @ElementList(entry = XMLMissionChangeData.MISSION_CHANGE_MATCHER, inline = true, required = true)
    public List<XMLMissionChangeData> changes = new ArrayList<>();

    public XMLMissionChangesData() {
    }

    public static List<FeedChange> fromCoT(Feed feed, CotEvent event) {
        List<FeedChange> ret = new ArrayList<>();
        if (event == null)
            return ret;

        // Get the MissionChanges detail
        CotDetail detail = event.findDetail("mission");
        if (detail == null)
            return ret;

        detail = detail.getFirstChildByName(0, MISSION_CHANGE_MATCHER);
        if (detail == null)
            return ret;

        XMLMissionChangesData container = null;
        Serializer serializer = new Persister();
        try {
            container = serializer.read(XMLMissionChangesData.class,
                    detail.toString());
        } catch (Exception e) {
            Log.e(TAG, "Failed to load changes: " + event, e);
        }

        if (container == null || container.changes == null)
            return ret;

        for (XMLMissionChangeData ch : container.changes)
            ret.add(FeedChange.fromXML(feed, ch));

        return ret;
    }
}
