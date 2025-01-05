package com.caci.apollo.datafeed;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.caci.apollo.datafeed.AnalyticStarterBroadcastReceiver.ANALYTIC_STARTER_BROADCAST;

/**
 * Helper class for Data Feed library
 */
public class DataFeedUtil {

    private static final String TAG = "Tommy DataFeedUtil";
    
    /**
     * @hidden
     */
    public DataFeedUtil() {
        // Data Feed is meant to be a static classs, don't show it in javadoc
    }

    /**
     * Set actvity life cycle to CREATE
     * @param ctx The android activity context (eg., type
     * AppCompatActivity).  Context is needed to get activity name as
     * well as sending broadcast.
     */
    public static void SetLifeCycleStateCreate(Context ctx) {
        String broadcastName = ANALYTIC_STARTER_BROADCAST;
        String activityName = ctx.getClass().getSimpleName();
        Intent intent = new Intent();
        intent.setAction (broadcastName);
        intent.putExtra("action", "ANALYTIC_RUNNING");
        intent.putExtra("activity", activityName);
        ctx.sendBroadcast(intent);
    }

    /**
     * Set actvity life cycle to DESTROY
     * @param ctx The android activity context (eg., type
     * AppCompatActivity).  Context is needed to get activity name as
     * well as sending broadcast.
     */
    public static void SetLifeCycleStateDestroy(Context ctx) {
        String broadcastName = ANALYTIC_STARTER_BROADCAST;
        String activityName = ctx.getClass().getSimpleName();
        Intent intent = new Intent();
        intent.setAction (broadcastName);
        intent.putExtra("action", "ANALYTIC_NOT_RUNNING");
        intent.putExtra("activity", activityName);
        ctx.sendBroadcast(intent);
    }
    
}

    
