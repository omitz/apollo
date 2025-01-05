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

package com.atakmap.android.missionapi.data;

import com.atakmap.android.missionapi.util.FeedUtils;
import com.atakmap.comms.TAKServer;
import com.atakmap.coremap.filesystem.FileSystemUtils;

/**
 * TAK server version
 */
public class TAKServerVersion {

    // Unknown version - used for placeholders
    public static final TAKServerVersion SUPPORT_UNKNOWN
            = new TAKServerVersion("", 0);

    public static final TAKServerVersion VER_1_3_13 = new TAKServerVersion("1.3.13", 3);

    // Feeds with authentication (such as passwords)
    public static final TAKServerVersion SUPPORT_AUTH = VER_1_3_13;

    // Feeds with user roles
    public static final TAKServerVersion SUPPORT_ROLES = VER_1_3_13;

    // Content-specific keywords
    public static final TAKServerVersion SUPPORT_CONTENT_KEYWORDS = VER_1_3_13;

    // New(er) invitations request that allows bulk invitations
    public static final TAKServerVersion SUPPORT_BULK_INVITE = VER_1_3_13;

    // All feed metadata (content, changes, logs) can be obtained in a single request
    public static final TAKServerVersion SUPPORT_BULK_FEED_DATA = VER_1_3_13;

    public int api, major, minor, build, revision;
    public String tag;

    public TAKServerVersion(String str, int apiVersion) {
        if (FileSystemUtils.isEmpty(str))
            return;

        this.api = apiVersion;

        int[] parts = new int[4];
        String[] split = str.split("\\.");
        for (int i = 0; i < 4 && i < split.length; i++) {
            try {
                String s = split[i];
                int end = s.length();
                for (int j = 0; j < s.length(); j++) {
                    char c = s.charAt(j);
                    if (c < '0' || c > '9') {
                        end = j;
                        break;
                    }
                }
                parts[i] = Integer.parseInt(s.substring(0, end));
            } catch (Exception ignore) {}
        }

        this.major = parts[0];
        this.minor = parts[1];
        this.build = parts[2];
        this.revision = parts[3];

        int lastTag = str.lastIndexOf('-');
        if (lastTag > -1)
            this.tag = str.substring(lastTag + 1);
    }

    public TAKServerVersion(TAKServer server) {
        this(server != null ? server.getServerVersion() : null,
                server != null ? server.getServerAPI() : 2);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.major).append(".");
        sb.append(this.minor).append(".");
        sb.append(this.build).append(".");
        sb.append(this.revision);
        if (this.tag != null)
            sb.append(this.tag);
        return sb.toString();
    }

    public boolean checkSupport(TAKServerVersion other) {
        if (other == null) return true;
        if (other == SUPPORT_UNKNOWN) return false;

        if (this.api > other.api) return true;
        if (this.api < other.api) return false;

        if (this.major > other.major) return true;
        if (this.major < other.major) return false;

        if (this.minor > other.minor) return true;
        if (this.minor < other.minor) return false;

        if (this.build > other.build) return true;
        if (this.build < other.build) return false;

        return this.revision >= other.revision;
    }

    public static boolean checkSupport(TAKServer server, TAKServerVersion version) {
        if (server == null)
            return false;
        return new TAKServerVersion(server).checkSupport(version);
    }

    public static TAKServerVersion fromNCS(String serverNCS) {
        return new TAKServerVersion(FeedUtils.findServer(serverNCS));
    }
}
