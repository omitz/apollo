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

package com.atakmap.android.missionapi.model.json;

/**
 * Interface for serializable JSON data that has a separate form client-side
 * and server-side
 *
 * The server-side variant support has been added to due to repeat issues
 * with the server misunderstanding client data, either due to older server
 * versions failing to parse new data or client exclusive data leaking into
 * server requests.
 */
public interface JSONSerializable {

    /**
     * Convert this object to a JSON object
     * @param server True if this data is meant for server consumption
     * @return JSON data
     */
    JSONData toJSON(boolean server);
}
