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

package com.atakmap.android.missionapi.view;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.atakmap.android.missionapi.util.IconUtils;

/**
 * Map item icon w/ attachment icon in bottom-right corner
 */
public class AttachmentDrawable extends Drawable {

    public static final int ALIGN_TOP_LEFT = 0;
    public static final int ALIGN_TOP_RIGHT = 1;
    public static final int ALIGN_BOTTOM_RIGHT = 2;
    public static final int ALIGN_BOTTOM_LEFT = 3;

    protected Drawable _baseDrawable, _attDrawable;
    protected int _align = ALIGN_BOTTOM_RIGHT;

    public AttachmentDrawable(Drawable baseDrawable, Drawable attDrawable) {
        _baseDrawable = baseDrawable;
        _attDrawable = attDrawable;
    }

    public AttachmentDrawable(Drawable baseDrawable) {
        this(baseDrawable, IconUtils.getCoreDrawable(
                com.atakmap.app.R.drawable.attachment));
    }
    
    public AttachmentDrawable(Resources res, Bitmap baseIcon, Bitmap attIcon) {
        this(new BitmapDrawable(res, baseIcon),
                new BitmapDrawable(res, attIcon));
    }

    public AttachmentDrawable(Resources res, Bitmap baseIcon) {
        this(new BitmapDrawable(res, baseIcon));
    }

    public void setBaseDrawable(Drawable baseDrawable) {
        if (baseDrawable != null && baseDrawable != _baseDrawable) {
            _baseDrawable = baseDrawable;
            invalidateSelf();
        }
    }

    public void setAttachmentDrawable(Drawable attDrawable) {
        if (attDrawable != _attDrawable) {
            _attDrawable = attDrawable;
            invalidateSelf();
        }
    }

    public void setBaseColor(int color) {
        _baseDrawable.setColorFilter(new PorterDuffColorFilter(color,
                PorterDuff.Mode.MULTIPLY));
        invalidateSelf();
    }

    public void setAttachmentColor(int color) {
        if (_attDrawable != null) {
            _attDrawable.setColorFilter(new PorterDuffColorFilter(color,
                    PorterDuff.Mode.MULTIPLY));
            invalidateSelf();
        }
    }

    public void setIconAlignment(int align) {
        if (_align != align) {
            _align = align;
            invalidateSelf();
        }
    }

    @Override
    public synchronized void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float width = bounds.width();
        float height = bounds.height();

        // Base icon
        _baseDrawable.draw(canvas);

        // Attachment badge
        int restore = canvas.save();
        float x = _align == ALIGN_TOP_RIGHT || _align == ALIGN_BOTTOM_RIGHT
                ? width : 0;
        float y = _align == ALIGN_BOTTOM_LEFT || _align == ALIGN_BOTTOM_RIGHT
                ? height : 0;
        canvas.scale(0.5f, 0.5f);
        canvas.translate(x, y);
        if (_attDrawable != null)
            _attDrawable.draw(canvas);
        canvas.restoreToCount(restore);
    }

    @Override
    public void setAlpha(int alpha) {
        // do nothing
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // do nothing
    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        _baseDrawable.setBounds(left, top, right, bottom);
        if (_attDrawable != null)
            _attDrawable.setBounds(left, top, right, bottom);
    }

    @Override
    public int getIntrinsicWidth() {
        return _baseDrawable.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return _baseDrawable.getIntrinsicHeight();
    }
}
