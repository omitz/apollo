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
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class XMLUserLog extends XMLMissionContent {

    @Element(name = "uid")
    public String uid;

    @Element(name = "resourceUid", required = false)
    public String resourceUid;

    @Element(name = "dtg")
    public String dtg;

    @Element(name = "log")
    public String log;

    @Element(name = "creatorUid", required = false)
    public String creatorUid;

    @Element(name = "servertime", required = false)
    public String servertime;

    @ElementList(entry = "contentHashes", inline = true, required = false)
    public Set<String> contentHashes = new HashSet<>();

    @ElementList(entry = "Keyword", inline = true, required = false)
    public List<String> keywords = new ArrayList<>();

}
