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

package com.atakmap.android.missionapi.net.http.delete;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.OpType;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.comms.http.TakHttpResponse;
import com.foxykeep.datadroid.exception.ConnectionException;

/**
 * Operation to HTTP DELETE a CoT UID
 *
 */
public class DeleteFeedOperation extends AbstractOperation {

    private static final String TAG = "DeleteFeedOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException {
        serverRequest(r, OpType.DELETE, b);
    }

    @Override
    protected void onServerResponse(AbstractRequest req, TakHttpResponse res,
                                    Bundle b) throws Exception {
        b.putString(RestManager.RESPONSE_JSON,
                res.getStringEntity(RestManager.MATCHER_MISSION));
    }
}
