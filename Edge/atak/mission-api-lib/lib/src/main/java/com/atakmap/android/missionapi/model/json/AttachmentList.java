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

package com.atakmap.android.missionapi.model.json;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for a published item (e.g. map item or user log), and any file attachments
 *
 * Created by byoung on 4/11/2016.
 */
public class AttachmentList {

    /**
     * the referenced UID (map item or user log)
     */
    private String mUid;

    /**
      * The server response for publishing the primary item (e.g. association CoT with a mission,
      * or publishing log to a mission)
     */
    private String mPrimaryResponse;

    /**
     * store information about attachments to include local path, hash, and server response for
     * association of that file/hash with a mission
     */
    private List<String> mLocalPaths;
    private List<String> mHashes;
    private List<String> mResponses;

    public AttachmentList() {
        this.mHashes = new ArrayList<String>();
        this.mResponses = new ArrayList<String>();
    }

    public AttachmentList(String uid, String primaryResponse,
                          List<String> localPaths,
                          List<String> hashes, List<String> responses) {
        this.mUid = uid;
        this.mPrimaryResponse = primaryResponse;
        this.mLocalPaths = localPaths;
        this.mHashes = hashes;
        this.mResponses = responses;
    }

    public String getUid() {
        return mUid;
    }

    public String getPrimaryResponse() {
        return mPrimaryResponse;
    }

    public boolean hasPrimaryResponse() {
        return !FileSystemUtils.isEmpty(mPrimaryResponse);
    }

    public List<String> getLocalPaths() {
        return mLocalPaths;
    }

    public boolean hasLocalPaths() {
        return !FileSystemUtils.isEmpty(mLocalPaths);
    }

    public List<String> getHashes() {
        return mHashes;
    }

    public boolean hasHashes() {
        return !FileSystemUtils.isEmpty(mHashes);
    }

    public List<String> getResponses() {
        return mResponses;
    }

    public boolean hasResponses() {
        return !FileSystemUtils.isEmpty(mResponses);
    }

    public boolean isValid() {
        return !FileSystemUtils.isEmpty(mUid);
    }

    public JSONObject toJSON() throws JSONException {
        if (!isValid())
            throw new JSONException("Invalid AttachmentList");

        JSONObject json = new JSONObject();
        json.put("uid", mUid);

        if (!FileSystemUtils.isEmpty(mPrimaryResponse))
            json.put("PrimaryResponse", mPrimaryResponse);

        JSONArray uidArray = new JSONArray();
        json.put("Hashes", uidArray);
        if (hasHashes()) {
            for (String s : mHashes) {
                uidArray.put(s);
            }
        }

        uidArray = new JSONArray();
        json.put("Responses", uidArray);
        if (hasResponses()) {
            for (String s : mResponses) {
                uidArray.put(s);
            }
        }

        uidArray = new JSONArray();
        json.put("LocalPaths", uidArray);
        if (hasLocalPaths()) {
            for (String s : mLocalPaths) {
                uidArray.put(s);
            }
        }

        return json;
    }

    public static AttachmentList fromJSON(JSONObject json)
            throws JSONException {
        AttachmentList r = new AttachmentList();
        r.mUid = json.getString("uid");

        if (json.has("PrimaryResponse"))
            r.mPrimaryResponse = json.getString("PrimaryResponse");

        if (json.has("Hashes")) {
            JSONArray array = json.getJSONArray("Hashes");
            List<String> jcontents = new ArrayList<String>();
            for (int i = 0; i < array.length(); i++) {
                String s = array.getString(i);
                if (FileSystemUtils.isEmpty(s))
                    throw new JSONException("Invalid JSON item");
                jcontents.add(s);
            }

            r.mHashes = jcontents;
        }

        if (json.has("Responses")) {
            JSONArray array = json.getJSONArray("Responses");
            List<String> jcontents = new ArrayList<String>();
            for (int i = 0; i < array.length(); i++) {
                String s = array.getString(i);
                if (FileSystemUtils.isEmpty(s))
                    throw new JSONException("Invalid JSON item");
                jcontents.add(s);
            }

            r.mResponses = jcontents;
        }

        if (json.has("LocalPaths")) {
            JSONArray array = json.getJSONArray("LocalPaths");
            List<String> jcontents = new ArrayList<String>();
            for (int i = 0; i < array.length(); i++) {
                String s = array.getString(i);
                if (FileSystemUtils.isEmpty(s))
                    throw new JSONException("Invalid JSON item");
                jcontents.add(s);
            }

            r.mLocalPaths = jcontents;
        }

        return r;
    }

    public static JSONObject toJSONList(List<AttachmentList> atts)
            throws JSONException {
        JSONObject json = new JSONObject();
        JSONArray uidArray = new JSONArray();
        json.put("Attachments", uidArray);
        if (!FileSystemUtils.isEmpty(atts)) {
            for (AttachmentList s : atts) {
                uidArray.put(s.toJSON());
            }
        }
        return json;
    }

    /**
     * Convert JSON to list of results
     *
     * @param json
     * @return
     * @throws JSONException
     */
    public static List<AttachmentList> fromJSONList(JSONObject json)
            throws JSONException {

        JSONArray array = json.getJSONArray("Attachments");
        ArrayList<AttachmentList> results = new ArrayList<AttachmentList>();
        if (array == null || array.length() < 1) {
            return results;
        }

        for (int i = 0; i < array.length(); i++) {
            AttachmentList r = AttachmentList.fromJSON(array.getJSONObject(i));
            if (r == null || !r.isValid())
                throw new JSONException("Invalid AttachmentList");
            results.add(r);
        }

        return results;
    }
}
