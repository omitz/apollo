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

package com.atakmap.android.missionapi.net.http;

import android.content.Context;
import android.os.Bundle;

import com.atakmap.android.http.rest.operation.HTTPOperation;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.comms.http.TakHttpResponse;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;
import com.foxykeep.datadroid.requestmanager.Request;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import java.io.IOException;

/**
 * In an ongoing effort to simplify this awful mess, this class represents
 * a generic mission HTTP operation
 */
public abstract class AbstractOperation extends HTTPOperation {

    private static final String TAG = "AbstractOperation";

    @Override
    public Bundle execute(Context context, Request request) throws
            ConnectionException, DataException {

        // Unfortunately this framework serializes and deserializes the request
        // between when it's created and when we get it back here
        // so we can't just cast the request to the class we need
        // Need to do reflection and deserialize from JSON. Yuck

        AbstractRequest aReq = AbstractRequest.fromRequest(request);
        if (aReq == null || !aReq.isValid())
            throw new DataException("Unable to serialize invalid query request");

        Bundle b = new Bundle();
        b.putString(RestManager.REQUEST_JSON, aReq.toJSON().toString());
        b.putInt(NetworkOperation.PARAM_STATUSCODE, HttpStatus.SC_OK);
        execute(context, aReq, b);
        return b;
    }

    /**
     * Convenience method for performing a server request
     * @param req Request container
     * @param opType Operation type (GET, PUT, or DELETE)
     * @param args Arguments for the endpoint
     * @throws ConnectionException On failure
     */
    protected void serverRequest(AbstractRequest req, OpType opType,
            Bundle b, String... args) throws ConnectionException {
        String missionUID = req.getFeedUID();
        String endPoint = req.getRequestEndpoint(args);
        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(req.getServerURL());

            String url = client.getUrl(endPoint);
            url = FileSystemUtils.sanitizeURL(url);

            Log.d(TAG, "Performing " + opType + " request on " + url);

            HttpRequestBase r = null;
            switch (opType) {
                case GET: r = new HttpGet(url); break;
                case PUT: r = new HttpPut(url); break;
                case POST: r = new HttpPost(url); break;
                case DELETE: r = new HttpDelete(url); break;
            }

            // Additional request headers
            addHeaders(r, req);

            // Include request body if applicable
            if (r instanceof HttpEntityEnclosingRequestBase) {
                String body = req.getRequestBody();
                if (!FileSystemUtils.isEmpty(body)) {
                    StringEntity se = new StringEntity(body);
                    se.setContentType(HttpUtil.MIME_JSON);
                    se.setContentEncoding(FileSystemUtils.UTF8_CHARSET.name());
                    ((HttpEntityEnclosingRequestBase) r).setEntity(se);
                }
            }

            // Handle server response
            onServerResponse(req, client.execute(r), b);
        } catch (TakHttpException e) {
            Log.e(TAG, "Failed to perform operation on: " + missionUID, e);
            throw new ConnectionException(e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform operation on: " + missionUID, e);
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

    protected void serverRequest(AbstractRequest req, OpType opType,
                                 String... args) throws ConnectionException {
        serverRequest(req, opType, null, args);
    }

    /**
     * Override this to handle the server HTTP response
     * @param req Request container
     * @param res Server response
     * @param b Response bundle
     */
    protected void onServerResponse(AbstractRequest req, TakHttpResponse res,
                                    Bundle b) throws Exception {
        res.verifyOk();
    }

    /**
     * Execute the operation using the initial request JSON
     *
     * @param context Activity context
     * @param request The initial request
     * @param response Response bundle
     */
    protected abstract void execute(Context context, AbstractRequest request,
            Bundle response) throws DataException, ConnectionException;

    /**
     * Execute request and get response string
     * @param req Initial request
     * @param url Request URL
     * @return Response body
     * @throws IOException Something went wrong
     */
    protected String get(AbstractRequest req, String url, String accept, String encoding)
            throws IOException {
        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(req.getServerURL());

            String queryUrl = client.getUrl(url);
            queryUrl = FileSystemUtils.sanitizeURL(queryUrl);

            HttpGet httpget = new HttpGet(queryUrl);
            if (!FileSystemUtils.isEmpty(accept))
                httpget.addHeader("Accept", accept);
            if (!FileSystemUtils.isEmpty(encoding))
                httpget.addHeader("Accept-Encoding", encoding);

            // Additional headers
            addHeaders(httpget, req);

            TakHttpResponse response = client.execute(httpget);
            response.verifyOk();
            return response.getStringEntity();
        } finally {
            try {
                if (client != null)
                    client.shutdown();
            } catch (Exception ignore) {}
        }
    }

    public String get(AbstractRequest req, String url) throws IOException {
        return get(req, url, null, null);
    }

    public String get(AbstractRequest req) throws IOException {
        return get(req, req.getRequestEndpoint());
    }

    protected String getGZip(AbstractRequest req, String url, String accept)
            throws IOException {
        return get(req, url, accept, HttpUtil.GZIP);
    }

    protected String getGZip(AbstractRequest req, String url)
            throws IOException {
        return getGZip(req, url, null);
    }

    protected String getGZip(AbstractRequest req)
            throws IOException {
        return getGZip(req, req.getRequestEndpoint());
    }

    protected static void addHeaders(HttpRequestBase r, AbstractRequest req) {
        r.addHeader("API_VERSION", String.valueOf(req.getAPIVersion()));
        if (req.hasToken())
            r.addHeader("Authorization", "Bearer " + req.getToken());
    }
}
