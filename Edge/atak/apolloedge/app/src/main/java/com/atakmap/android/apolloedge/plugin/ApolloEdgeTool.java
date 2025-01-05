
package com.atakmap.android.apolloedge.plugin;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.android.tools.BadgeDrawable;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import transapps.mapi.MapView;
import transapps.maps.plugin.tool.Group;
import transapps.maps.plugin.tool.Tool;
import transapps.maps.plugin.tool.ToolDescriptor;

/**
 * The Tool implementation within ATAK is just an ActionBar Button
 * that can be selected.  In most implementations a tool just launches
 * the DropDown Receiver.  If the plugin has not forward facing user
 * drop down, this can be omitted.
 */
public class ApolloEdgeTool extends Tool implements ToolDescriptor {

    /*
     * Member Variables:
     */
    public static final String TAG = "ApolloEdgeTool";
    private final Context context;
    private final LayerDrawable _icon;


    /*
     * Constructor:
     */
    public ApolloEdgeTool(final Context context) {
        this.context = context;

        _icon = (LayerDrawable) context.getResources().getDrawable
            (R.drawable.ic_launcher_badge, context.getTheme());
    }


    /*
     * Member functions:
     */
    private static void setBadgeCount(Context context, LayerDrawable icon, int count) {
        BadgeDrawable badge;

        // Reuse drawable if possible
        Drawable reuse = icon.findDrawableByLayerId(R.id.ic_badge);
        if (reuse instanceof BadgeDrawable) {
            badge = (BadgeDrawable) reuse;
        } else {
            badge = new BadgeDrawable(context);
        }

        badge.setCount(count);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.ic_badge, badge);
    }


    @Override
    public String getDescription() {
        return context.getString(R.string.app_name);
    }

    @Override
    public Drawable getIcon() {
        return _icon;
    }

    @Override
    public Group[] getGroups() {
        return new Group[] {
            Group.GENERAL
        };
    }

    @Override
    public String getShortDescription() {
        // remember to internationalize your code
        return context.getString(R.string.app_name);
    }
    
    @Override
    public Tool getTool() {
        return this;
    }

    @Override
    public void onActivate(Activity arg0, MapView arg1, ViewGroup arg2,
                           Bundle arg3, ToolCallback arg4) {

        // Hack to close the dropdown that automatically opens when a tool
        // plugin is activated.
        if (arg4 != null) {
            arg4.onToolDeactivated(this);
        }

        // Intent to launch the dropdown or tool
        arg2.setVisibility(ViewGroup.INVISIBLE);
        Intent intent = new Intent ("com.atakmap.android.apolloedge.SHOW_PLUGIN");
        AtakBroadcast.getInstance().sendBroadcast(intent);
    }

    @Override
    public void onDeactivate(ToolCallback arg0) {
    }
}
