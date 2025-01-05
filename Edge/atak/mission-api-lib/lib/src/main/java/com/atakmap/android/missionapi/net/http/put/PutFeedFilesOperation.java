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

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import com.atakmap.android.missionapi.data.FeedManager;
import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.android.missionapi.model.json.FeedFile;
import com.atakmap.android.missionapi.model.json.FeedFileDetails;
import com.atakmap.android.missionapi.net.http.AbstractOperation;
import com.atakmap.android.missionapi.net.http.AbstractRequest;
import com.atakmap.android.missionapi.net.http.RestManager;
import com.atakmap.android.missionapi.net.http.commons.CountingFileEntity;
import com.atakmap.android.missionapi.net.http.commons.IStreamListener;
import com.atakmap.android.filesystem.ResourceFile;
import com.atakmap.android.http.rest.DownloadProgressTracker;
import com.atakmap.android.http.rest.operation.NetworkOperation;
import com.atakmap.android.math.MathUtils;
import com.atakmap.android.missionapi.notifications.NotificationBuilder;
import com.atakmap.android.missionapi.notifications.RecentActivity;
import com.atakmap.android.missionapi.util.TimeUtils;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.comms.http.HttpUtil;
import com.atakmap.comms.http.TakHttpClient;
import com.atakmap.comms.http.TakHttpResponse;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.filesystem.HashingUtils;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;

import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generic operation to PUT files and then associate hashes with a feed
 */
public class PutFeedFilesOperation extends AbstractOperation {

    private static final String TAG = "PutFeedFilesOperation";

    @Override
    public void execute(Context context, AbstractRequest r, Bundle b)
            throws ConnectionException {

        // Get request data
        PutFeedFilesRequest req = (PutFeedFilesRequest) r;

        int fileCount = 0, currentProgress = 0;

        try {
            // setup progress notifications
            NotificationManager notifyManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationBuilder builder = new NotificationBuilder();
            String message = "Publishing " + req.getFiles().size()
                    + " files to " + req.getFeedName() + "...";
            builder.setContentTitle(message)
                    .setContentText(message)
                    .setSmallIcon(
                            com.atakmap.android.util.NotificationUtil.GeneralIcon.SYNC_ORIGINAL
                                    .getID());

            ArrayList<String> responses = new ArrayList<>();
            ArrayList<String> hashes = new ArrayList<>();
            for (ResourceFile resourceFile : req.getFiles()) {
                currentProgress = (int) Math.round((double) fileCount
                        / (double) req.getFiles().size() * 100) + 1;
                fileCount++;

                if (!FileSystemUtils.isFile(resourceFile.getFilePath())) {
                    throw new DataException("File does not exist: "
                            + resourceFile.getFilePath());
                }

                File currentFile = new File(resourceFile.getFilePath());

                message = String.format(LocaleUtil.getCurrent(),
                        "Publishing %d of %d files (%d%%) - %s - %s", fileCount,
                        req.getFiles().size(),
                        currentProgress, req.getFeedName(),
                        currentFile.getName());

                if (req.getFiles().size() > 1
                        && req.getNotificationID() > 0) {
                    builder.setProgress(currentProgress, 100);
                    builder.setContentText(message);
                    notifyManager.notify(req.getNotificationID(),
                            builder.build());
                }
                Log.d(TAG, message + ". " + req.toString());

                // Obtain mime type using content type and extension
                String mimeType = resourceFile.getMimeType();
                String contentType = resourceFile.getContentType();
                if (FileSystemUtils.isEmpty(mimeType)) {
                    String path = resourceFile.getFilePath();
                    ResourceFile.MIMEType mt = ResourceFile
                            .getMIMETypeForFile(path);
                    if (mt != null)
                        mimeType = mt.MIME;
                    else
                        mimeType = ResourceFile.UNKNOWN_MIME_TYPE;
                }

                Pair<String, String> p = publish(context, req,
                        currentFile, mimeType, contentType);
                hashes.add(p.first);
                responses.add(p.second);
            }

            message = "Published " + req.getFiles().size() + " files to "
                    + req.getFeedName();
            Log.d(TAG, message + ". " + req.toString());
            //display multi-file notification if more than 1 file, and a notify ID is provided
            if (req.getFiles().size() > 1
                    && req.getNotificationID() > 0) {
                notifyManager.cancel(req.getNotificationID());
                RecentActivity.notify(req.getFeedUID(), message, message);
            }

            b.putStringArrayList(RestManager.RESPONSE_JSON, responses);
            b.putStringArrayList(RestManager.RESPONSE_HASHES, hashes);
        } catch (ConnectionException e) {
            Log.e(TAG, "Failed to put", e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Failed to put", e);
            throw new ConnectionException(e);
        }
    }

    /**
     * Upload file, if hash not already available on the server. Then associate hash with the
     * specified feed, both in local feed, and on remote server DB
     *
     * @param req Request
     * @param file File to publish
     * @param mimeType File MIME type
     * @param contentType File content type
     * @return  2 strings: hash, and JSON response
     * @throws ConnectionException
     * @throws DataException
     */
    public static Pair<String, String> publish(final Context context,
            AbstractRequest req, final File file, String mimeType,
            String contentType)
            throws ConnectionException, DataException {

        String feedUID = req.getFeedUID();
        String feedName = req.getFeedName();
        if (FileSystemUtils.isEmpty(mimeType)) {
            Log.w(TAG, "Publishing file " + file + " to feed "
                    + feedUID + " with unknown MIME type!");
            mimeType = ResourceFile.UNKNOWN_MIME_TYPE;
        }

        Log.d(TAG, "Computing hash of: " + file.getAbsolutePath());

        //first get hash of file
        //TODO could we cache attachment hashes somewhere so they don't have
        //would need to track modified timestamp and recompute hash if file changes
        String hash = HashingUtils.sha256sum(file);
        if (FileSystemUtils.isEmpty(hash)) {
            throw new DataException(
                    "Failed to create hash for: " + file.getAbsolutePath());
        }

        int statusCode = NetworkOperation.STATUSCODE_UNKNOWN;
        final int notifyId = req.getNotificationID();
        TakHttpClient client = null;
        try {
            client = TakHttpClient.GetHttpClient(req.getServerURL());

            //first check if hash already exists on server...
            String hashUrl = client.getUrl("sync/content");
            Uri.Builder builder = Uri.parse(hashUrl).buildUpon();
            builder.appendQueryParameter("hash", hash);
            hashUrl = builder.build().toString();
            hashUrl = FileSystemUtils.sanitizeURL(hashUrl);

            HttpHead httpHead = new HttpHead(hashUrl);
            Log.d(TAG, "Checking if file already on server "
                    + httpHead.getRequestLine());
            TakHttpResponse response = client.execute(httpHead);
            statusCode = response.getStatusCode();

            if (response.isOk()) {
                Log.d(TAG, "Hash already exists on server: " + hash);
                if (!FileSystemUtils.isEmpty(contentType)) {
                    //set content type keyword with a prefix we can match on later
                    String contentTypeKeyword = FeedFileDetails.CONTENT_TYPE
                            + contentType;
                    setKeywordsForHash(req, hash, contentTypeKeyword, client);
                }
            } else {
                final long fileSize = file.length();
                Log.d(TAG, "Hash not on server, uploading: " + hash
                        + " of size: " + fileSize);

                final NotificationManager notifyManager = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                final NotificationBuilder nbuilder = new NotificationBuilder();
                String message = "Publishing " + file.getName() + " to "
                        + feedName + "...";
                nbuilder.setContentTitle(message)
                        .setContentText(message)
                        .setSmallIcon(NotificationUtil.GeneralIcon
                                .SYNC_ORIGINAL.getID());

                //hash not found, upload file
                String postUrl = client.getUrl("sync/upload");
                builder = Uri.parse(postUrl).buildUpon();
                String filename = FileSystemUtils
                        .sanitizeFilename(file.getName());
                builder.appendQueryParameter("name", filename);
                String creatorUid = req.getCreatorUID();
                if (!FileSystemUtils.isEmpty(creatorUid))
                    builder.appendQueryParameter("creatorUid", creatorUid);
                if (!FileSystemUtils.isEmpty(contentType)) {
                    //set content type keyword with a prefix we can match on later
                    String contentTypeKeyword = FeedFileDetails.CONTENT_TYPE
                            + contentType;
                    builder.appendQueryParameter("keywords",
                            contentTypeKeyword);
                }
                //TODO include location? UID? etc?
                postUrl = builder.build().toString();
                postUrl = FileSystemUtils.sanitizeURL(postUrl);

                HttpPost post = new HttpPost(postUrl);
                post.addHeader("Content-Type", mimeType);
                //httppost.addHeader("Content-Length", String.valueOf(file.length()));

                // Add additional request headers
                addHeaders(post, req);

                //increase notification id to not stomp the multi-file upload status
                final DownloadProgressTracker progressTracker = new DownloadProgressTracker(
                        fileSize);

                CountingFileEntity fileEntity = new CountingFileEntity(file,
                        mimeType);
                fileEntity.setListener(new IStreamListener() {

                    @Override
                    public void numRead(int numRead) {
                        //TODO this is called b/f the bytes are written, so progress may be a bit off

                        // see if we should update progress notification based on progress or time since
                        // last update
                        long currentTime = System.currentTimeMillis();
                        if (progressTracker.contentReceived(numRead,
                                currentTime)) {
                            String innerMessage = String.format(
                                    LocaleUtil.getCurrent(),
                                    "Publishing %s (%d%% of %s) %s, %s remaining",
                                    file.getName(),
                                    progressTracker.getCurrentProgress(),
                                    MathUtils.GetLengthString(fileSize),
                                    MathUtils.GetDownloadSpeedString(
                                            progressTracker
                                                    .getAverageSpeed()),
                                    MathUtils.GetTimeRemainingString(
                                            progressTracker
                                                    .getTimeRemaining()));
                            if (notifyId > 0) {
                                nbuilder.setProgress(progressTracker
                                        .getCurrentProgress(), 100);
                                nbuilder.setContentText(innerMessage);
                                notifyManager.notify(notifyId, nbuilder.build());
                            }
                            Log.d(TAG, innerMessage);
                            // start a new block
                            progressTracker.notified(currentTime);
                        }
                    }
                });

                post.setEntity(fileEntity);

                Log.d(TAG,
                        "executing request " + post.getRequestLine()
                                + " with MIME="
                                + mimeType + ", content=" + contentType);
                response = client.execute(post);

                // check response for HTTP 200
                statusCode = response.getStatusCode();
                if (!response.isOk()) {
                    Log.w(TAG, "HTTP file operation failed: "
                            + response.toString());
                    String errorMessage = response.getReasonPhrase();
                    if (FileSystemUtils.isEmpty(errorMessage))
                        errorMessage = "HTTP file operation failed with code: "
                                + statusCode;
                    throw new IOException(errorMessage);
                }

                //TODO match response content?
                //TODO parse JSON response and be sure they got the right hash?

                message = "Published " + file.getName() + " to " + feedName;
                if (notifyId > 0) {
                    notifyManager.cancel(notifyId);
                    RecentActivity.notify(feedUID, message, message);
                }
            } //end file upload

            //now associate with feed
            String responseBody = associateWithFeed(req, file,
                    hash, client);

            return new Pair<>(hash, responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Failed to put: " + hash, e);
            throw new ConnectionException(e.getMessage(), statusCode);
        } finally {
            try {
                if (client != null)
                    client.shutdown();
            } catch (Exception ignore) {
            }
        }
    }

    private static String associateWithFeed(AbstractRequest req,
            File file, String hash, TakHttpClient httpclient)
            throws IOException {

        //find feed
        String feedName = req.getFeedName();
        String creatorUid = req.getCreatorUID();
        Feed feed = FeedManager.getInstance().getFeed(req.getFeedUID());
        if (feed == null || !feed.isValid()) {
            Log.w(TAG, "Feed not found, cannot associate: "
                    + file.getAbsolutePath() + ", with: " + feed);
            throw new IOException("Feed not found, cannot associate: "
                    + file.getName() + ", with: " + feedName);
        }

        //see if hash is already in local feed
        boolean bNewFile = false;
        FeedFile entry = feed.getContent(hash);
        if (entry == null) {
            //add to feed
            Log.d(TAG, "Adding " + hash + ", to local feed: " + feed);
            bNewFile = true;

            //generate some temporary details, will replace with server details once confirmed
            String timestamp = TimeUtils.getTimestamp();
            entry = Feed.createFile(feed);
            entry.name = file.getName();
            entry.size = file.length();
            entry.hash = hash;
            entry.setTimestamp(timestamp);
            entry.creatorUID = creatorUid;
            feed.addContent(entry);
        }

        entry.setLocalFile(file);

        //now associate file with server feed
        String responseBody = null;
        try {
            responseBody = associateWithServerFeed(req,
                    Collections.singletonList(hash), httpclient);

            //now set content for that entry, based on server response
            Feed newFeed = RestManager.getFeed(req, responseBody);
            if (newFeed == null || !newFeed.isValid()) {
                throw new IOException(
                        "Failed to parse server response for file: "
                                + file.getName());
            }

            FeedFile newEntry = newFeed.getContent(hash);
            if (newEntry == null || !newEntry.isValid()) {
                throw new IOException(
                        "Failed to parse server metadata response for file: "
                                + file.getName());
            }
            entry.setDetails(newEntry.getDetails());
        } catch (Exception e) {
            Log.e(TAG, "Failed to associate: " + hash + ", " + bNewFile, e);
            //failed to store in server feed, remove from local feed
            if (bNewFile)
                feed.removeContent(hash);
        } finally {
            try {
                if (httpclient != null)
                    httpclient.shutdown();
            } catch (Exception ignore) {
            }
        }

        feed.persist();
        return responseBody;
    }

    static String associateWithServerFeed(AbstractRequest req,
                                          List<String> hashes, TakHttpClient httpclient)
            throws IOException, JSONException {

        //now associate file/hash with this feed
        //TODO fault tolerance... how to handle upload/post error?
        String feedName = req.getFeedName();
        String creatorUid = req.getCreatorUID();
        String putUrl = httpclient
                .getUrl("api/missions/" + feedName + "/contents");
        if (!FileSystemUtils.isEmpty(creatorUid)) {
            Uri.Builder builder = Uri.parse(putUrl).buildUpon();
            builder.appendQueryParameter("creatorUid", creatorUid);
            putUrl = builder.build().toString();
        }
        putUrl = FileSystemUtils.sanitizeURL(putUrl);

        HttpPut put = new HttpPut(putUrl);
        put.addHeader("Content-Type", HttpUtil.MIME_JSON);
        put.addHeader("Accept-Encoding", HttpUtil.GZIP);

        // Add additional request headers
        addHeaders(put, req);

        String requestBody = getRequestBody(hashes);
        StringEntity se = new StringEntity(requestBody);
        se.setContentType(HttpUtil.MIME_JSON);
        se.setContentEncoding(FileSystemUtils.UTF8_CHARSET.name());
        put.setEntity(se);

        Log.d(TAG, "Associating hashes: " + requestBody + ", with feed "
                + feedName + ", " + put.getRequestLine());
        // post file
        TakHttpResponse response = httpclient.execute(put);
        response.verifyOk();
        return response.getStringEntity(RestManager.MATCHER_MISSION);
    }

    /**
     * set the 'keywords' metadata for the specified file
     * @param hash
     * @param keywords
     * @param httpclient
     * @throws IOException
     */
    public static boolean setKeywordsForHash(AbstractRequest req, String hash,
            String keywords, TakHttpClient httpclient)
            throws IOException {

        if(FileSystemUtils.isEmpty(hash)){
            Log.w(TAG, "setKeywordsForHash no hash");
            return false;
        }

        String putUrl = httpclient
                .getUrl("api/sync/metadata/" + hash + "/keywords");
        putUrl = FileSystemUtils.sanitizeURL(putUrl);

        HttpPut put = new HttpPut(putUrl);
        put.addHeader("Content-Type", HttpUtil.MIME_JSON);

        // Add additional request headers
        addHeaders(put, req);

        String requestBody = "[\"";
        if(!FileSystemUtils.isEmpty(keywords)){
            requestBody += keywords;
        }
        requestBody += "\"]";

        StringEntity se = new StringEntity(requestBody);
        se.setContentType(HttpUtil.MIME_JSON);
        se.setContentEncoding(FileSystemUtils.UTF8_CHARSET.name());
        put.setEntity(se);

        Log.d(TAG, "Associating keywords: " + requestBody + ", with hash "
                + hash + ", " + put.getRequestLine());
        // PUT metadata
        TakHttpResponse response = httpclient.execute(put);
        response.verifyOk();
        return true;
    }

    /**
     * Get request body in JSON format
     * @param hashes
     * @return
     * @throws JSONException
     */
    public static String getRequestBody(List<String> hashes)
            throws JSONException {
        //ensure at least one thing to publish
        if (FileSystemUtils
                .isEmpty(hashes) /*&& FileSystemUtils.isEmpty(uids)*/) {
            throw new JSONException("No data to publish");
        }

        JSONObject json = new JSONObject();

        if (!FileSystemUtils.isEmpty(hashes)) {
            JSONArray hashArray = new JSONArray();
            json.put("hashes", hashArray);
            for (String hash : hashes) {
                hashArray.put(hash);
            }
        }

        return json.toString();
    }
}
