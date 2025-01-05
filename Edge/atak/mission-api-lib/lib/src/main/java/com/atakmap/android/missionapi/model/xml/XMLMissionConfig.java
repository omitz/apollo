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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.List;

public class XMLMissionConfig {

    /**
     * hostname:port:proto
     */
    @Attribute(name = "serverConnectString", required = true)
    public String serverConnectString;

    /**
     * True to delete local content during app shutdown
     * False to persist locally cached data
     */
    @Attribute(name = "deleteContentDuringShutdown", required = true)
    public boolean deleteContentDuringShutdown;

    /**
     * Optional mission name, must exist on referenced server
     */
    @Element(name = "Mission", required = false)
    public XMLServerConfig mission;

    /**
     * Optional bounding box to geographically restrict search space
     */
    @Element(name = "BBox", required = false)
    public XMLBoundingBox bbox;

    /**
     * Optional bounding box to geographically restrict search space
     */
    @Element(name = "TimeRange", required = false)
    public XMLDateTimeRange timeRange;

    /**
     * Optional list of MIME Type filters
     */
    @ElementList(entry = "MIMEType", inline = true, required = false)
    public List<String> mimeTypes = new ArrayList<>();

    /**
     * Optional list of Keyword filters
     */
    @ElementList(entry = "Keyword", inline = true, required = false)
    public List<String> keywords = new ArrayList<>();
}
