
package com.atakmap.android.apolloedge;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.http.rest.ServerVersion;
import com.atakmap.android.importexport.ExporterManager;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.ipc.DocumentedExtra;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.cot.UIDHandler;
import com.atakmap.android.dropdown.DropDownMapComponent;
import com.atakmap.android.maps.graphics.GLMapItemFactory;

import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapEventDispatcher.MapEventDispatchListener;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.munitions.DangerCloseReceiver;
import com.atakmap.android.user.geocode.GeocodeManager;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;

import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.apolloedge.plugin.R;
import com.atakmap.app.preferences.ToolsPreferenceFragment;

import com.atakmap.android.cot.CotMapComponent;

import com.atakmap.android.radiolibrary.RadioMapComponent;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.net.DeviceProfileClient;

import android.content.IntentFilter;
import android.location.Address;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * This is an example of a MapComponent within the ATAK 
 * ecosphere.   A map component is the building block for all
 * activities within the system.   This defines a concrete 
 * thought or idea. 
 */
public class ApolloEdgeMapComponent extends DropDownMapComponent {

    /*
     * Member Variables:
     */
    private Context                    pluginContext;
    private ApolloEdgeMapOverlay       mapOverlay;
    private ApolloEdgeDropDownReceiver dropDown;

    public static final String TAG = "ApolloEdgeMapComponent";

    
    /*
     * Member Functions:
     */
    @Override
    public void onCreate(final Context context, Intent intent, final MapView view) {

        /*
         * 1.) Set the theme.  Otherwise, the plugin will look vastly
         * different than the main ATAK experience.  The theme needs
         * to be set programatically because the AndroidManifest.xml
         * is not used.  Copy styles.xml, colors.xml, and dimen.xml to
         * app/src/main/res/values/
         */
        context.setTheme(R.style.ATAKPluginTheme);

        /*
         * 2.) Get plugin context.
         */ 
        super.onCreate(context, intent, view);
        pluginContext = context;

        /*
         * 3.) Create a map overlay.
         */ 
        this.mapOverlay = new ApolloEdgeMapOverlay(view, pluginContext);
        view.getMapOverlayManager().addOverlay(this.mapOverlay);

        /*
         * 4.) Create and register dropDown receiver.  
         */ 
        // In this example, a drop down receiver is the visual
        // component within the ATAK system.  The trigger for this
        // visual component is an intent.  see the
        // plugin.ApolloEdgeTool where that intent is triggered.
        // See ApolloEdgeDropDownReceiver.java where the intent is recieved.
        // see also hello_world_layout.xml


        // hack to workaround ATAK removeOnCotEventListener bug.
        Random rand = new Random(System.currentTimeMillis());
        String instanceID = "" + rand.nextInt();
        ServerVersion serverVersion = new ServerVersion("hello", instanceID);
        CotMapComponent.getInstance().setServerVersion(serverVersion);
        serverVersion = CotMapComponent.getInstance().getServerVersion("hello");
        Log.d (TAG,"serverversion = " +  serverVersion.getVersion());


        
        this.dropDown = new ApolloEdgeDropDownReceiver(view, context, this.mapOverlay);
        // We use documented intent filters within the system
        // in order to automatically document all of the 
        // intents and their associated purposes.
        Log.d(TAG, "registering the show apollo edge filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(ApolloEdgeDropDownReceiver.SHOW_PLUGIN,
                "Show the Aollo Edge drop-down");
        this.registerDropDownReceiver(this.dropDown, ddFilter);
        Log.d(TAG, "registered the show apolloe edge filter");

        // // TBF 
        // IntentFilter filter = new IntentFilter (ApolloEdgeDropDownReceiver.UPDATE_MODEL_HASH);
        // this.pluginContext.registerReceiver (this.dropDown, filter);

        
        /*
         * 7.) Connect and register to the ATAK server.
         */
        //see if any hello profiles/data are available on the TAK
        //Server. Requires the server to be properly configured, and
        //"Apply TAK Server profile updates" setting enabled in ATAK
        //prefs
        // Log.d(TAG, "Checking for Hello profile on TAK Server");
        // DeviceProfileClient.getInstance().getProfile(view.getContext(), "apollo");

        //register profile request to run upon connection to TAK Server, in case we're not yet
        //connected, or the the request above fails
        // CotMapComponent.getInstance().addToolProfileRequest("apollo");
    }


    @Override
    public void onStart(final Context context, final MapView view) {
        Log.d(TAG, "onStart");
    }

    @Override
    public void onPause(final Context context, final MapView view) {
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume(final Context context,
            final MapView view) {
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStop(final Context context,
            final MapView view) {
        Log.d(TAG, "onStop");
    }


    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        Log.d(TAG, "calling on destroy");
        // this.pluginContext.unregisterReceiver (this.dropDown); // TBF
        
        this.dropDown.dispose();
        view.getMapOverlayManager().removeOverlay(mapOverlay);
        super.onDestroyImpl(context, view);

        // Example call on how to end ATAK if the plugin is unloaded.
        // It would be important to possibly show the user a dialog etc.
        
        //Intent intent = new Intent("com.atakmap.app.QUITAPP");
        //intent.putExtra("FORCE_QUIT", true);
        //AtakBroadcast.getInstance().sendBroadcast(intent);

    }
}
