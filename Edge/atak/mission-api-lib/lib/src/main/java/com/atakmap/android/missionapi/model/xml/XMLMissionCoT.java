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

import java.util.Set;

public class XMLMissionCoT extends XMLMissionContent {

    @Element(name = "Metadata", required = false)
    public XMLContentUidData _metadata;

    @ElementList(entry = "contentHashes", inline = true, required = false)
    public Set<String> contentHashes;

}
