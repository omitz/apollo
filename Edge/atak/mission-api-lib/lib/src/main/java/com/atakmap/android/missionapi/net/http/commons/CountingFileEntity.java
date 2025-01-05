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

package com.atakmap.android.missionapi.net.http.commons;

import org.apache.commons.io.output_mod.CountingOutputStream;
import org.apache.http.entity.FileEntity;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by byoung on 9/14/17.
 */

public class CountingFileEntity extends FileEntity {

    private IStreamListener listener;

    public CountingFileEntity(File file, String contentType) {
        super(file, contentType);
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        CountingOutputStream output = new CountingOutputStream(out) {
            @Override
            protected void beforeWrite(int n) {
                //pass off number of bytes to be read in the current block
                if (listener != null && n != 0) {
                    listener.numRead(n);
                    //use this to get cummulative rather than current block
                    //listener.numRead(getByteCount());
                }
                super.beforeWrite(n);
            }
        };
        super.writeTo(output);

    }

    public void setListener(IStreamListener listener) {
        this.listener = listener;
    }

    public IStreamListener getListener() {
        return listener;
    }

}
