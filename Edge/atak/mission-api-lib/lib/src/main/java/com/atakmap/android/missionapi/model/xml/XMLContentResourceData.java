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

import org.simpleframework.xml.Element;

public class XMLContentResourceData {

    @Element(required = true)
    public String uid;

    @Element(required = true)
    public String name;

    @Element(required = true)
    public String hash;

    @Element(required = false)
    public String filename;

    @Element(required = false)
    public String submissionTime;

    //info for who/when uploaded this data to the server
    @Element(required = false)
    public String creatorUid;

    @Element(required = false)
    public String submitter;

    @Element(required = false)
    public String tool;

    @Element(required = false)
    public String mimeType;

    @Element(required = false)
    public String contentType;

    @Element(name = "keywords", required = false)
    public String keywordString;

    @Element(name = "keywordArray", required = false)
    public String[] keywords;

    @Element(required = false)
    public long size;

    //info for who/when associated this data with the mission
    @Element(required = false)
    public XMLCreatorData creatorData;
}
