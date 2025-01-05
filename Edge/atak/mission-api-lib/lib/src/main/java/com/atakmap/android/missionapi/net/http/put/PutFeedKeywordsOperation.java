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
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.notifications.RecentActivity;
import com.atakmap.android.missionapi.R;
import com.atakmap.android.hashtags.util.HashtagSet;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.missionapi.util.StringUtils;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpException;
import com.atakmap.comms.http.TakHttpResponse;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;

/**
 * First DELETE keywords
 * Then PUT keywords
 *
 */
public class PutFeedKeywordsOperation extends AbstractOperation {

    private static final String TAG = "PutFeedKeywordsOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException,
            DataException {

        // Get request data
        PutFeedKeywordsRequest req = (PutFeedKeywordsRequest) r;

        String response = "";
        HashtagSet tags = req.getHashtags();
        if (tags.isEmpty()) {
            // DELETE existing keywords
            String message = String.format(LocaleUtil.getCurrent(),
                    "Deleting keywords for %s", req.getFeedName());
            Log.d(TAG, message + ". " + req);
            serverRequest(req, OpType.DELETE);
        } else {
            String message = String.format(LocaleUtil.getCurrent(),
                    "Setting keywords for %s", req.getFeedName());
            Log.d(TAG, message + ". " + req);

            // PUT updated keywords
            message = String.format(LocaleUtil.getCurrent(),
                    "Keywords updated for %s", req.getFeedName());
            RecentActivity.notifySuccess(req.getFeedUID(),
                    StringUtils.getString(
                            R.string.mn_publish_hashtags_complete,
                            req.getFeedName()),
                    StringUtils.getString(
                            R.string.mn_publish_hashtags_complete_msg,
                            req.getFeedName(),
                            req.getServerURL()));
            Log.d(TAG, message + ". " + req);
            response = publish(req);
            //Log.d(TAG, "Response: " + response);
        }

        b.putString(RestManager.RESPONSE_JSON, response);
    }

    private String publish(PutFeedKeywordsRequest req)
            throws ConnectionException,
            DataException {

        String feedName = req.getFeedName();
        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(req.getServerURL());

            //see if this has a locally generated UID, or a server UID has been assigned
            //TODO fault tolerance... how to handle upload/post error?
            String requestUrl = client.getUrl(req.getRequestEndpoint());
            requestUrl = FileSystemUtils.sanitizeURL(requestUrl);

            HttpPut httpPut = new HttpPut(requestUrl);
            httpPut.addHeader("Content-Type", HttpUtil.MIME_JSON);
            httpPut.addHeader("Accept-Encoding", HttpUtil.GZIP);

            // Add additional request headers
            addHeaders(httpPut, req);

            //TODO pre-calculate hashes and publish log message prior to posting attachment files (similar to how we handle CoT publish)?
            //so other users can get the log message sooner
            String requestBody = getRequestBody(req);
            StringEntity se = new StringEntity(requestBody);
            se.setContentType(HttpUtil.MIME_JSON);
            se.setContentEncoding(FileSystemUtils.UTF8_CHARSET.name());
            httpPut.setEntity(se);

            Log.d(TAG,
                    httpPut.getClass() + ", associating keywords" + requestBody
                            + " with feed " + feedName + ", "
                            + httpPut.getRequestLine());

            // post (or put) file
            TakHttpResponse response = client.execute(httpPut);
            response.verifyOk();
            String ret = response.getStringEntity();
            return ret;
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

    private static String getRequestBody(PutFeedKeywordsRequest req) {

        HashtagSet tags = req.getHashtags();
        JSONArray json = new JSONArray();
        if (tags.isEmpty()) {
            Log.d(TAG, "getRequestBody, no keywords");
            return json.toString();
        }

        for (String keyword : tags) {
            if (FileSystemUtils.isEmpty(keyword)) {
                Log.w(TAG, "Skipping invalid keyword");
                continue;
            }
            json.put(keyword);
        }

        return json.toString();
    }
}
