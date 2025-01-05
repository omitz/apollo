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

import com.atakmap.coremap.log.Log;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Deprecated data storage format for missions
 * Strictly to be used for converting to JSON
 */
@Root(name = "MissionManifest")
public class XMLMissionManifest {

    private static final String TAG = "XMLMissionManifest";

    @Attribute(name = "version", required = true)
    public int version = 1;

    /**
     * Unique ID for this search. This UID is local to this machine
     */
    @Attribute(name = "uid", required = true)
    public String uid;

    /**
     * User label for this search
     */
    @Attribute(name = "label", required = true)
    public String label;

    /**
     * Timestamp this device/search was last synced with the server
     */
    @Attribute(name = "lastSyncTime", required = true)
    public long lastSyncTime;

    @Attribute(name = "lastAccessTime", required = false)
    public long lastAccessTime;

    @Element(name = "Config", required = true)
    public XMLMissionConfig config;

    @Element(name = "Content", required = false)
    public XMLMissionContents contents;

    @Attribute(name = "notifications", required = false)
    public boolean notifications = true;

    // Deprecated - Kept as a global preference
    @Attribute(name = "showRecceLogsOnly", required = false)
    public boolean userLogsOnly = true;

    public static XMLMissionManifest fromXml(String xml) {
        if (xml == null)
            return null;
        Serializer serializer = new Persister();
        try {
            XMLMissionManifest manifest = serializer.read(
                    XMLMissionManifest.class, xml);
            return manifest;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load Mission: " + xml, e);
            return null;
        }
    }
}
