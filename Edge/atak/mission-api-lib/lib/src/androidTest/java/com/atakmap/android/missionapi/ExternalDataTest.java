/*
 * Copyright 2021 PAR Government Systems
 *
 * Unlimited Rights:
 * PAR Government retains ownership rights to this software.  The Government has Unlimited Rights
 * to use, modify, reproduce, release, perform, display, or disclose this
 * software as identified in the purchase order contract. Any
 * reproduction of computer software or portions thereof marked with this
 * legend must also reproduce the markings. Any person who has been provided
 * access to this software must be aware of the above restrictions.
 */

package com.atakmap.android.missionapi;

import com.atakmap.android.data.URIContentManager;
import com.atakmap.android.data.URIContentSender;
import com.atakmap.android.missionapi.model.json.FeedExternalDetails;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Test/example of how to publish external data to a feed
 */
public class ExternalDataTest extends AbstractHTTPTest {

    @Test
    public void externalDataTest() {
        // Create external data details
        FeedExternalDetails details = new FeedExternalDetails();

        // Title for this data
        details.name = "Link Title";

        // String to show in the change log (optional)
        // In Data Sync this message replaces the "User added content" message
        details.notes = "User added some test data!";

        // Name of the plugin/tool associated with this data
        details.tool = "Tool Name";

        // UID tied to this data (for this example just use a random string)
        details.uid = UUID.randomUUID().toString();

        // URL that's used for displaying this data (usually in Data Sync)
        // This should point to a brief barebones HTML representation of
        // the data for users that don't have the associated plugin installed
        details.urlView = "http://example.com/";

        // URL that points to the underlying data associated with this link
        // This is the URI that's used for content handler lookup
        details.urlData = "http://example.com/";

        // Convert to base-64 URI for transport across the content manager
        String uri = details.toContentURI();

        // Get the list of supported senders
        List<URIContentSender> senders = URIContentManager.getInstance().getSenders(uri);

        // Make sure there's at least 1 supported sender
        // If the Data Sync plugin is loaded, it should show up in this list
        assertFalse("No send handlers found for external data", senders.isEmpty());

        // Send using the first handler in the list
        senders.get(0).sendContent(uri, new URIContentSender.Callback() {
            @Override
            public void onSentContent(URIContentSender sender, String uri, boolean success) {
                // Content is finished being sent
                assertTrue("Failed to publish external data to feed", success);
            }
        });

        // Example of how to use a tile button dialog to prompt the user for
        // which sender to use. If using Data Sync, this will also prompt the
        // user to select which feed to publish to, with the option to create
        // a new feed if needed.
        /*
        new SendDialog.Builder(mapView)
                .setName(details.name)
                .setURI(uri)
                .show();
        */

        // If you have a specific feed ready, you can publish external data
        // directly to the feed like this:
        //RestManager.getInstance().executeRequest(new PutExternalDataRequest(feed, details));
    }
}
