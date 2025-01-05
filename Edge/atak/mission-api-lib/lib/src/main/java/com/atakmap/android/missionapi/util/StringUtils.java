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

package com.atakmap.android.missionapi.util;

import android.content.Context;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;

import java.util.Collection;

/**
 * Helper methods related to string searching
 */
public class StringUtils {

    private static Context _libCtx;

    public static void init(Context libCtx) {
        _libCtx = libCtx;
    }

    /**
     * Get a resource-based string
     * @param libResId Library-specific resource ID
     * @param args String format arguments
     * @return String
     */
    public static String getString(int libResId, Object... args) {
        return _libCtx.getString(libResId, args);
    }

    public static boolean find(String str, String terms) {
        return !FileSystemUtils.isEmpty(str) && str.toLowerCase(
                LocaleUtil.getCurrent()).contains(terms);
    }

    public static boolean find(String str, Collection<String> terms) {
        for (String term : terms) {
            if (!FileSystemUtils.isEmpty(str) && !FileSystemUtils.isEmpty(term)
                    && str.toLowerCase(LocaleUtil.getCurrent()).contains(term))
                return true;
        }

        return false;
    }

    public static boolean find(Collection<String> lhs, Collection<String> rhs) {
        return find(getHashtagsString(lhs), rhs)
                || find(getHashtagsString(rhs), lhs);
    }

    /**
     * Convert array of hashtags to a single string
     * @param tags Tag array
     * @return Tag string
     */
    public static String getHashtagsString(final String... tags) {
        if (FileSystemUtils.isEmpty(tags))
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            if (!tag.startsWith("#"))
                sb.append('#');
            sb.append(tag);
            if (i < tags.length - 1)
                sb.append(' ');
        }
        return sb.toString();
    }

    public static String getHashtagsString(final Collection<String> tags) {
        if (tags == null || tags.isEmpty())
            return "";

        return getHashtagsString(tags.toArray(new String[0]));
    }
}
