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

import com.atakmap.coremap.maps.coords.GeoPoint;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

import java.util.List;

/**
 * XML-based attributes for CoT details such as type, callsign, etc.
 */
public class XMLContentUidDetails {

    @Attribute(required = false)
    public String type;

    @Attribute(required = false)
    public String callsign;

    @Attribute(required = false)
    public String title; // unused - ignore

    @Attribute(required = false)
    public String iconsetPath;

    @Attribute(required = false)
    public Integer color;

    @Attribute(required = false)
    public String category;

    @Attribute(required = false)
    public String name;

    @ElementList(entry = "attachment", inline = true, required = false)
    public List<String> attachments;

    @Element(required = false)
    public XMLContentLocation location;

    public GeoPoint getLocation() {
        return this.location != null ? this.location.toGeoPoint() : null;
    }
}
