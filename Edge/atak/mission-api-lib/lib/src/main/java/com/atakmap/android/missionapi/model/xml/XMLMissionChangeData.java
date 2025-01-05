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

import com.atakmap.android.missionapi.model.json.FeedChange;

import org.simpleframework.xml.Element;

/**
 * A change to mission.
 * For ADD_CONTENT or REMOVE_CONTENT, either a CoT/UID or a "Resource" JSON is required
 * If we have contentUid, then details is optional.
 *
 * Note would have liked to use MissionContentUidData here, but could not convince TAK Server
 * team to structure data the same in the MissionChange as in the MissionData
 *
 * Created by byoung on 3/11/2016.
 */
public class XMLMissionChangeData {

    private static final String TAG = "MissionChangeData";
    public static final String MISSION_CHANGE_MATCHER = "MissionChange";
    public static final String MISSION_CHANGE_LOG = "MISSION_CHANGE_LOG";

    @Element(required = true)
    public FeedChange.Type type;

    // The time this change was published by the client
    @Element(required = true)
    public String timestamp;

    // The time this change was acknowledged by the server
    @Element(required = false)
    public String serverTime;

    @Element(required = true)
    public String missionName;

    @Element(required = true)
    public String creatorUid;

    /**
     * Either contentResource or contentUid is required
     * If we have contentUid, then details is optional
     */
    @Element(required = false)
    public XMLContentResourceData contentResource;

    @Element(required = false)
    public String contentUid;

    @Element(required = false)
    public XMLContentUidDetails details;

    /**
     * External data params
     */
    @Element(required = false)
    public XMLExternalDetails externalData;

    /**
     * User log entry (only used when publishing offline changes)
     */
    @Element(required = false)
    public XMLUserLog logEntry;

    public XMLMissionChangeData() {
    }
}
