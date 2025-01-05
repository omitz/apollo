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

import com.atakmap.android.missionapi.model.json.Feed;
import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Parcelable for a request which can be retry upon failure
 * 
 * @author byoung
 */
public abstract class RetryRequest extends AbstractRequest {

    private static final String TAG = "RetryRequest";

    /**
     * Note, if other operations end up needing to be delayed, lets make this more generic and build
     * it into the HTTP Operation base classes. E.g. could implement a "FixedDelay" that sleeps for
     * a set amount of time
     * 
     * @author byoung
     */
    public interface OperationDelay {

        /**
         * Delay this operation (put this thread to sleep) based on the number of times this
         * operation has previously failed
         */
        void delay();
    }

    /**
     * How many times has this request been attempted
     */
    private int _retryCount;

    protected RetryRequest(Feed feed, int retryCount) {
        super(feed);
        _retryCount = retryCount;
    }

    protected RetryRequest(JSONObject json) throws JSONException {
        super(json);
        _retryCount = json.getInt("RetryCount");
    }

    public OperationDelay getDelay() {
        return new BackoffDelay(_retryCount);
    }

    public boolean isValid() {
        return _retryCount >= 0;
    }

    public int getRetryCount() {
        return _retryCount;
    }

    public void setRetryCount(int count) {
        _retryCount = count;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("RetryCount", getRetryCount());
        } catch (Exception e) {
            Log.e(TAG, "Failed to serialize to JSON", e);
        }
        return json;
    }

    /**
     * Operation delay that backs off more as number of attempts increases
     * 
     * @author byoung
     */
    static class BackoffDelay implements OperationDelay {

        private final int _numberAttempts;

        public BackoffDelay(int numberAttempts) {
            _numberAttempts = numberAttempts;
        }

        /**
         * Delay this operation (put this thread to sleep) based on the number of times this
         * operation has previously failed
         */
        @Override
        public void delay() {
            if (_numberAttempts <= 1)
                return;

            long delaySeconds = 0;
            if (_numberAttempts == 2)
                delaySeconds = 1;
            else if (_numberAttempts == 3)
                delaySeconds = 3;
            else if (_numberAttempts == 4)
                delaySeconds = 5;
            else if (_numberAttempts == 5)
                delaySeconds = 10;
            else if (_numberAttempts == 6)
                delaySeconds = 20;
            else if (_numberAttempts < 10)
                delaySeconds = 30;
            else if (_numberAttempts < 15)
                delaySeconds = 45;
            else if (_numberAttempts < 20)
                delaySeconds = 60;
            else
                delaySeconds = 90;

            try {
                Log.d(TAG, "Request being delayed " + delaySeconds
                        + " seconds...");
                Thread.sleep(delaySeconds * 1000);
            } catch (InterruptedException e) {
                Log.w(TAG, "Delay interrupted", e);
            }
        }
    }
}
