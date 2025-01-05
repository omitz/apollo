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

package com.atakmap.android.missionapi.util;

import android.net.Uri;
import android.widget.Toast;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.http.rest.ServerContact;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.routes.Route;
import com.atakmap.android.toolbars.RangeAndBearingEndpoint;
import com.atakmap.android.util.ServerListDialog;
import com.atakmap.android.widgets.AngleOverlayShape;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.SslNetCotPort;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.TAKServerListener;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper methods related to feeds
 */
public class FeedUtils {

    private static final String TAG = "FeedUtils";

    public static final String NULL = "null";

    public static final String DATA_FOLDER = FileSystemUtils.TOOL_DATA_DIRECTORY
            + File.separatorChar + "datasync";
    public static final String INCOMING_FILE_FOLDER = DATA_FOLDER
            + File.separatorChar + "incoming";
    public static final File TMP_DIR = FileSystemUtils.getItem(
            DATA_FOLDER + File.separator + "tmp");

    // Map item types that cannot be published
    private static final Set<String> TYPE_BLACKLIST = new HashSet<>(Arrays.asList(
            "self",
            Route.WAYPOINT_TYPE,
            Route.CONTROLPOINT_TYPE,
            "b-m-p-s-p-i",
            "shape_marker"
    ));

    /**
     * Base notification ID used by this library
     */
    public final static int NOTIFY_ID = 68500;

    public static String createUid(NetConnectString ncs, String name) {
        if (ncs == null) {
            Log.w(TAG, "createUid invalid server");
            return null;
        }
        int port = SslNetCotPort.getServerApiPort(SslNetCotPort.Type.UNSECURE);
        if (ncs.getProto() != null && ncs.getProto().equals("ssl"))
            port = SslNetCotPort.getServerApiPort(SslNetCotPort.Type.SECURE);
        return ncs.getHost() + "-" + port + "-" + ncs.getProto() + "-" + name;
    }

    public static String createUid(String serverNCS, String name) {
        return createUid(NetConnectString.fromString(serverNCS), name);
    }

    public static boolean contains(List<?> list, Object o) {
        for (Object v : list) {
            if (v.equals(o))
                return true;
        }
        return false;
    }

    public static MapItem findMapItem(String uid) {
        MapView mv = MapView.getMapView();
        if (mv == null)
            return null;
        MapGroup group = mv.getRootGroup();
        return group != null ? group.deepFindUID(uid) : null;
    }

    /**
     * Convert URL to net connect string
     * @param url URL string (i.e. https://example.com:8443)
     * @return Net connect string object (i.e. example.com:8443:ssl)
     */
    public static NetConnectString URLtoNCS(String url) {
        if (url == null)
            return null;

        Uri uri = Uri.parse(url);
        String proto = "tcp";
        String host = uri.getHost();
        int port = SslNetCotPort.getServerApiPort(SslNetCotPort.Type.UNSECURE);
        if (uri.getScheme() != null && uri.getScheme().equals("https")) {
            proto = "ssl";
            port = SslNetCotPort.getServerApiPort(SslNetCotPort.Type.SECURE);
        }
        return new NetConnectString(proto, host, port);
    }

    public static String NCStoURL(TAKServer server) {
        return NCStoURL(server.getConnectString());
    }

    public static String NCStoURL(String server) {
        return NCStoURL(NetConnectString.fromString(server));
    }

    /**
     * Convert net connect string to URL
     * @param ncs Net connect string (i.e. example.com:8443:ssl)
     * @return URL string (i.e. https://example.com)
     */
    public static String NCStoURL(NetConnectString ncs) {
        return ServerListDialog.getBaseUrl(ncs);
    }

    /**
     * Find TAK server based on connect string host and protocol
     * Ignores port since this usually differs between missions API and TAK server
     * @param server Server connect string or URL
     * @return Server CoT port or null if not found/invalid
     */
    public static TAKServer findServer(String server) {
        return TAKServerListener.getInstance().findServer(server);
    }

    /**
     * Find feed's associated TAK server
     * @param feed Feed
     * @return Server CoT port or null if not found/invalid
     */
    public static TAKServer findServer(Feed feed) {
        return findServer(feed != null ? feed.serverNCS : null);
    }

    public static void post(Runnable r) {
        MapView mv = MapView.getMapView();
        if (mv != null)
            mv.post(r);
    }

    public static void toast(final String str) {
        post(new Runnable() {
            @Override
            public void run() {
                MapView mv = MapView.getMapView();
                if (mv != null)
                    Toast.makeText(mv.getContext(), str, Toast.LENGTH_LONG)
                            .show();
            }
        });
    }

    /**
     * Create toast message using plugin string and arguments
     * @param stringId Plugin string ID
     * @param args Arguments
     */
    public static void toast(int stringId, Object... args) {
        toast(StringUtils.getString(stringId, args));
    }

    /**
     * Get a feeds's notification ID based on its UID
     * @param feedUID Feed UID
     * @return Notification ID
     */
    public static int getFeedNotificationId(String feedUID) {
        int id = 0;
        if (!FileSystemUtils.isEmpty(feedUID)) {
            id = feedUID.hashCode() % 65536;
            if (id <= 0)
                id += 65536;
        }
        return NOTIFY_ID + id;
    }

    /**
     * Find user callsign from UID
     * @param userUID UID
     * @param feed Feed (for reading server connect string)
     * @return User callsign or null if not found
     *         If UID is "anonymous" then "Anonymous user" is returned
     */
    public static String getUserCallsign(String userUID, Feed feed) {
        return getUserCallsign(userUID, feed, false);
    }

    public static String getUserCallsign(String userUID, Feed feed,
            boolean bReturnUidIfNotFound) {
        MapView mv = MapView.getMapView();
        if (mv == null)
            return "";

        if (FileSystemUtils.isEmpty(userUID))
            return bReturnUidIfNotFound ? userUID : null;

        // Self
        if (FileSystemUtils.isEquals(MapView.getDeviceUid(), userUID))
            return mv.getDeviceCallsign();

        // Anonymous user
        if (userUID.equalsIgnoreCase("anonymous"))
            return StringUtils.getString(R.string.anonymous_user);

        // Lookup using CoT marker - more recent than server contacts list
        MapItem mi = findMapItem(userUID);
        if (mi != null && mi.hasMetaValue("callsign"))
            return mi.getMetaString("callsign", null);

        // Lookup using server contacts list - less recent but more reliable
        TAKServer server = findServer(feed);
        ServerContact creator = null;
        if (server != null) {
            try {
                creator = CotMapComponent.getInstance().getServerContact(
                        server.getConnectString(), userUID);
            } catch (Exception ignore) {
            }
        }

        // Lookup using any server, e.g. cached mission data from a server that we are not
        // currently connected to
        if (creator == null) {
            try {
                creator = CotMapComponent.getInstance().getServerContact(
                        null, userUID);
            } catch (Exception ignore) {
            }
        }

        if (creator != null)
            return creator.getCallsign();

        if (userUID.startsWith("CN=")) {
            String cn = parseCN(userUID);
            if (!FileSystemUtils.isEmpty(cn))
                return cn;
        }

        //Log.w(TAG, "No callsign match for: " + userUID);
        return bReturnUidIfNotFound ? userUID : null;
    }

    /**
     * Parse CN from user String e.g. "CN=brian.l.young@gmail.com,O=TAK,ST=MA,C=US"
     * would return "brian.l.young@gmail.com"
     *
     * @param userString
     * @return
     */
    private static String parseCN(String userString) {
        if (FileSystemUtils.isEmpty(userString))
            return null;

        int startIndex = userString.indexOf("CN=");
        int endIndex = userString.indexOf(",");

        if (startIndex >= 0 && endIndex > 0) {
            return userString.substring(startIndex + 3, endIndex);
        }

        return null;
    }

    /**
     * Check if this map item can be published to the server
     * Typically excludes team markers, SPIs, or anything that can't be
     * serialized to CoT
     * @param item Map item
     * @return True if the item can be published, false if not allowed
     */
    public static boolean canPublish(MapItem item) {
        if (item == null)
            return false;

        // Item is not meant to be archived
        if (!item.hasMetaValue("archive"))
            return false;

        // No SPIs or self marker
        if (TYPE_BLACKLIST.contains(item.getType()))
            return false;

        // No team markers, emergency markers, file map items, or things
        // that can't be serialized
        if (item.hasMetaValue("atakRoleType") || item.hasMetaValue("nevercot")
                || item.hasMetaValue("emergency"))
            return false;

        // No R&B endpoints or bullseye shape
        if (item instanceof RangeAndBearingEndpoint
                || item instanceof AngleOverlayShape)
            return false;

        return true;
    }
}
