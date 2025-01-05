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

import android.net.Uri;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for URI path w/ parameter support
 */
public class URIPathBuilder {

    private List<String> paths = new ArrayList<>();
    private List<String> params = new ArrayList<>();

    public URIPathBuilder(String path) {
        addPath(path);
    }

    public URIPathBuilder addPath(String path) {
        if (FileSystemUtils.isEmpty(path))
            return this;
        String[] parts = path.split("/");
        for (String p : parts) {
            if (!p.isEmpty())
                paths.add(p);
        }
        return this;
    }

    public URIPathBuilder addParam(String key, String value) {
        if (!FileSystemUtils.isEmpty(value))
            addParam(key + "=" + Uri.encode(value));
        return this;
    }

    public URIPathBuilder addParam(String key, int value) {
        return addParam(key, String.valueOf(value));
    }

    public URIPathBuilder addParam(String key, long value) {
        return addParam(key, String.valueOf(value));
    }

    public URIPathBuilder addParam(String key, boolean value) {
        return addParam(key, String.valueOf(value));
    }

    public URIPathBuilder addParam(String param) {
        params.add(param);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String p : paths)
            sb.append("/").append(p);
        for (int i = 0; i < params.size(); i++)
            sb.append(i == 0 ? '?' : '&').append(params.get(i));
        return sb.toString();
    }
}
