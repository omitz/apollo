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

package com.atakmap.android.missionapi.net.http.put;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.OpType;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.comms.http.TakHttpResponse;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;

import org.apache.http.HttpStatus;

/**
 * HTTP REST operation for publishing an external data reference to a feed
 */
public class PutExternalDataOperation extends AbstractOperation {

    private static final String TAG = "PutExternalDataOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException, DataException {

        // Get request data
        PutExternalDataRequest req = (PutExternalDataRequest) r;

        // POST to server
        serverRequest(req, OpType.POST, b);
    }

    @Override
    protected void onServerResponse(AbstractRequest req, TakHttpResponse res, Bundle b) throws Exception {
        res.verify(HttpStatus.SC_CREATED);
        b.putString(RestManager.RESPONSE_JSON, res.getStringEntity(req.getMatcher()));
    }
}
