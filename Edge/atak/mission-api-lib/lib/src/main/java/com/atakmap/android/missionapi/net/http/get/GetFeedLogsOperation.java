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

import com.atakmap.android.missionapi.model.json.FeedLog;
import com.atakmap.android.missionapi.model.json.JSONData;
import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;

import java.util.List;

/**
 * Generic operation to GET logs for a feed, and update the local DB with those logs
 * Full replace of existing log list
 */
public class GetFeedLogsOperation extends AbstractOperation {

    private static final String TAG = "GetFeedLogsOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException {

        List<FeedLog> logs;
        String response;
        try {
            response = getGZip(r, r.getRequestEndpoint(), HttpUtil.MIME_JSON);

            //parse logs
            JSONData data = new JSONData(response, true);
            logs = data.getList("data", r.getFeed(), FeedLog.class);
            Log.d(TAG, "Parsed log list of size: " + logs.size());
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to query log list", e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to query log list: " + r.getFeedName(), e);
            throw new ConnectionException(e.getMessage(),
                    NetworkOperation.STATUSCODE_UNKNOWN);
        }
        b.putString(RestManager.RESPONSE_JSON, response);
    }
}
