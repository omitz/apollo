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
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.comms.http.TakHttpResponse;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;

import java.io.File;

/**
 * Operation to HTTP DELETE a CoT UID
 *
 */
public class PutFeedOperation extends AbstractOperation {

    private static final String TAG = "PutFeedOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException {

        // Get request data
        PutFeedRequest req = (PutFeedRequest) r;
        String feedName = r.getFeedName();

        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(r.getServerURL());

            String pathString = FileSystemUtils.sanitizeURL(
                    r.getRequestEndpoint());

            HttpPut httpPut = new HttpPut(client.getUrl(pathString));
            Log.d(TAG, "Creating/editing feed " + feedName + ", "
                    + httpPut.getRequestLine());

            if (req.getPackagePath() != null && req.getPackagePath().length() > 0) {
                httpPut.addHeader("Content-Type", HttpUtil.MIME_ZIP);
                httpPut.addHeader("Accept-Encoding", HttpUtil.GZIP);
            }

            // Add additional request headers
            addHeaders(httpPut, r);

            if (req.getPackagePath() != null && req.getPackagePath().length() > 0) {
                FileEntity fe = new FileEntity(new File(req.getPackagePath()),
                        HttpUtil.MIME_ZIP);
                httpPut.setEntity(fe);
            }

            TakHttpResponse response = client.execute(httpPut);
            int code = response.getStatusCode();
            if (code != HttpStatus.SC_OK && code != HttpStatus.SC_CREATED)
                response.verifyOk();
            b.putString(RestManager.RESPONSE_JSON,
                    response.getStringEntity(
                            RestManager.MATCHER_MISSION));
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to put: " + feedName, e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to put: " + feedName, e);
            throw new ConnectionException(e.getMessage(),
                    NetworkOperation.STATUSCODE_UNKNOWN);
        } finally {
            try {
                if (client != null)
                    client.shutdown();
            } catch (Exception ignore) {
            }
        }
    }
}
