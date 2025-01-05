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

import org.simpleframework.xml.ElementList;

import java.util.ArrayList;
import java.util.List;

public class XMLMissionContents {

    @ElementList(entry = "Log", inline = true, required = false)
    public List<XMLUserLog> userLogs = new ArrayList<>();

    @ElementList(entry = "CoT", inline = true, required = false)
    public List<XMLMissionCoT> cot = new ArrayList<>();

    @ElementList(entry = "File", inline = true, required = false)
    public List<XMLMissionFile> files = new ArrayList<>();

    @ElementList(entry = "ExternalData", inline = true, required = false)
    public List<XMLExternalData> externalData = new ArrayList<>();
}
