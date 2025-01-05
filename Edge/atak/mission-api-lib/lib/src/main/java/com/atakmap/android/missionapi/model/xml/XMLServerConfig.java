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

public class XMLServerConfig {

    /**
     * The name of mission, must exist on the server
     */
    @Attribute(name = "mission", required = true)
    public String mission;

    /**
     * The mission description
     */
    @Attribute(name = "description", required = false)
    public String description;

    /**
     * UID of the user who created the mission
     */
    @Attribute(name = "creatorUid", required = false)
    public String creatorUid;

    /**
     * Time the mission was created on the server
     */
    @Attribute(name = "createTime", required = false)
    public String createTime;

    /**
     * The chat room address e.g. XMPP conference JID
     */
    @Attribute(name = "chatroom", required = false)
    public String chatroom;

    /**
     * Public or private group
     */
    @Attribute(name = "group", required = false)
    public String group;

    /**
     * Mission is password protected
     */
    @Attribute(name = "passwordProtected", required = false)
    public boolean passwordProtected;

    /**
     * Default user role
     */
    @Attribute(name = "defaultRole", required = false)
    public String defaultRole;

    /**
     * True to subscribe to receive updates from the server
     */
    @Attribute(name = "subscribe", required = true)
    public boolean subscribe;
}
