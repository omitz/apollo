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

package com.atakmap.android.missionapi.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;

import com.atakmap.android.maps.MapView;

/**
 * Notification builder
 */
public class NotificationBuilder {

    private static Context _libCtx;

    public static void init(Context libCtx) {
        _libCtx = libCtx;
    }

    private final Context _context, _plugin;
    private final Notification.Builder _builder;

    public NotificationBuilder() {
        this(MapView.getMapView(), _libCtx);
    }

    public NotificationBuilder(MapView mapView, Context plugin) {
        _context = mapView.getContext();
        _plugin = plugin;
        if (android.os.Build.VERSION.SDK_INT < 26)
            _builder = new Notification.Builder(_context);
        else
            _builder = new Notification.Builder(_context,
                    "com.atakmap.app.def");
    }

    public NotificationBuilder setSmallIcon(int iconId) {
        _builder.setSmallIcon(iconId);
        return this;
    }

    public NotificationBuilder setContentTitle(String title) {
        _builder.setContentTitle(title);
        return this;
    }

    public NotificationBuilder setContentTitle(int stringId, Object... args) {
        return setContentTitle(_plugin.getString(stringId, args));
    }

    public NotificationBuilder setContentText(String text) {
        _builder.setContentText(text);
        return this;
    }

    public NotificationBuilder setContentText(int stringId, Object... args) {
        return setContentText(_plugin.getString(stringId, args));
    }

    public NotificationBuilder setProgress(int progress, int max) {
        _builder.setProgress(max, progress, false);
        return this;
    }

    public NotificationBuilder setAutoCancel(boolean autoCancel) {
        _builder.setAutoCancel(autoCancel);
        return this;
    }

    public NotificationBuilder setContentIntent(PendingIntent intent) {
        _builder.setContentIntent(intent);
        return this;
    }

    public NotificationBuilder setTicker(String ticker) {
        _builder.setTicker(ticker);
        return this;
    }

    public NotificationBuilder setWhen(long when) {
        _builder.setWhen(when);
        return this;
    }

    public Notification build() {
        return _builder.build();
    }
}
