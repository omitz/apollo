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

package com.atakmap.android.missionapi.net.http.put;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.OpType;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.comms.http.TakHttpResponse;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;

import org.apache.http.HttpStatus;

/**
 * Operation to HTTP PUT a feed subscription (or DELETE it)
 *
 */
public class PutFeedSubscriptionOperation extends AbstractOperation {
    
    private static final String TAG = "PutFeedSubscriptionOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException,
            DataException {
        PutFeedSubscriptionRequest req = (PutFeedSubscriptionRequest) r;
        serverRequest(req, req.isSubscribe() ? OpType.PUT : OpType.DELETE, b);
    }

    @Override
    protected void onServerResponse(AbstractRequest r, TakHttpResponse res,
                                    Bundle b) throws Exception {
        PutFeedSubscriptionRequest req = (PutFeedSubscriptionRequest) r;
        int code = res.getStatusCode();
        if (code == HttpStatus.SC_UNAUTHORIZED
                || code == HttpStatus.SC_FORBIDDEN) {
            b.putInt(NetworkOperation.PARAM_STATUSCODE, res.getStatusCode());
            b.putString(RestManager.RESPONSE_JSON, res.getStringEntity());
            return;
        }
        res.verify(req.isSubscribe() ? HttpStatus.SC_CREATED
                : HttpStatus.SC_OK);
        b.putString(RestManager.RESPONSE_JSON, res.getStringEntity());
    }
}
