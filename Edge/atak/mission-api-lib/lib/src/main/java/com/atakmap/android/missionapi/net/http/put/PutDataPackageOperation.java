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
import com.foxykeep.datadroid.exception.DataException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;

import java.io.File;

/**
 * Operation to publish a Data Package to the server
 */
public class PutDataPackageOperation extends AbstractOperation {

    private static final String TAG = "PutDataPackageOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException, DataException {

        // Get request data
        PutDataPackageRequest req = (PutDataPackageRequest) r;

        publish(req, req.getPackagePath(), b);
    }

    protected void publish(AbstractRequest req,
            String packagePath, Bundle b) throws ConnectionException {

        String feedName = req.getFeedName();
        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(req.getServerURL());

            String putUrl = client.getUrl(req.getRequestEndpoint());
            putUrl = FileSystemUtils.sanitizeURL(putUrl);

            HttpPut httpput = new HttpPut(putUrl);
            httpput.addHeader("Content-Type", HttpUtil.MIME_ZIP);
            httpput.addHeader("Accept-Encoding", HttpUtil.GZIP);

            // Add additional request headers
            addHeaders(httpput, req);

            FileEntity fe = new FileEntity(new File(packagePath),
                    HttpUtil.MIME_ZIP);
            httpput.setEntity(fe);

            Log.d(TAG, "Publish Data Package " + packagePath
                    + " with feed " + feedName + ", " + httpput.getRequestLine());
            onServerResponse(req, client.execute(httpput), b);
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

    @Override
    protected void onServerResponse(AbstractRequest req, TakHttpResponse res,
                                    Bundle b) throws Exception {
        b.putString(RestManager.RESPONSE_JSON, res.getStringEntity());
        b.putInt(NetworkOperation.PARAM_STATUSCODE, res.getStatusCode());
    }
}
