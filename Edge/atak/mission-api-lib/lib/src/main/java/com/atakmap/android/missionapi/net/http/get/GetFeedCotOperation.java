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

package com.atakmap.android.missionapi.net.http.get;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.http.rest.operation.GetCotHistoryOperation;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic operation to GET all current CoT Event for a given feed
 */
public class GetFeedCotOperation extends AbstractOperation {

    private static final String TAG = "GetFeedCotOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException {

        // Get request data
        GetFeedCotRequest req = (GetFeedCotRequest) r;

        try {
            String xml = getGZip(req, req.getRequestEndpoint(),
                    HttpUtil.MIME_XML);

            List<CotEvent> cotEvents = new ArrayList<>();
            if (req.hasSingleUID()) {
                CotEvent event = CotEvent.parse(xml);
                if (event != null)
                    cotEvents.add(event);
            } else {
                cotEvents = GetCotHistoryOperation.parseCotEvents(
                        xml, req.getMatcher(), req.getStartTime(),
                        req.getWhiteList());
            }

            Log.d(TAG, "Parsed CoT history of size: " + cotEvents.size());

            b.putSerializable(RestManager.RESPONSE_JSON,
                    cotEvents.toArray(new CotEvent[0]));
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to query CoT Event: "
                    + req.getFeedName(), e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to query CoT Event: "
                    + req.getFeedName(), e);
            throw new ConnectionException(e.getMessage(),
                    NetworkOperation.STATUSCODE_UNKNOWN);
        }
    }
}
