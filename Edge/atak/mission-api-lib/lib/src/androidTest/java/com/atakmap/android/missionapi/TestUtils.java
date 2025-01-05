package com.atakmap.android.missionapi;

import com.atakmap.comms.CotServiceRemote.Proto;
import com.atakmap.comms.NetConnectString;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.TAKServerListener;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;

import static org.junit.Assert.*;

public class TestUtils {

    private static TAKServer _server;

    /**
     * Find an appropriate test server by using the first connected TAK server
     * If the user has no TAK servers configured, the test will fail
     * @return TAK Server or null if none
     */
    public static TAKServer getTestServer() {
        if (_server == null) {
            int attemptCount = 0;
            TAKServer[] servers = null;
            while (FileSystemUtils.isEmpty(servers) && attemptCount++ < 60) {
                try {
                    servers = TAKServerListener.getInstance()
                            .getConnectedServers();
                } catch (Exception ignored) {
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception ignored) {
                }
            }

            assertFalse(FileSystemUtils.isEmpty(servers));

            _server = servers[0];
        }
        return _server;
    }

    /**
     * Get the URL for the TAK server
     * @return TAK server URL
     */
    public static String getTestURL() {
        TAKServer server = getTestServer();
        NetConnectString ncs = NetConnectString.fromString(
                server.getConnectString());
        assertNotNull(ncs);

        if (Proto.ssl.toString().equals(ncs.getProto()))
            return "https://" + ncs.getHost();
        return "http://" + ncs.getHost();
    }

    /**
     * Get the testing directory used for any file tests
     * @return Test directory
     */
    public static File getTestDir() {
        File dir = FileSystemUtils.getItem(FileSystemUtils.TOOL_DATA_DIRECTORY
                + "/mission-api/tests");
        if (!dir.exists() && !dir.mkdirs())
            fail("Failed to make directory: " + dir);
        return dir;
    }
}
