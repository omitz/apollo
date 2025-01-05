package apollo_utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class UtilsLocation {

    private LocationManager locationManager;
    private List<String> providers;
    private List<ApolloLocationListener> locationListeners;

    public UtilsLocation(Activity activity) {
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        // Get providers, e.g. LocationManager.GPS_PROVIDER and LocationManager.NETWORK_PROVIDER
        providers = locationManager.getProviders(true); // passive, gps, network
        locationListeners = new ArrayList<>();
        // Create a listener for each provider
        for (String provider : providers) {
            locationListeners.add(new ApolloLocationListener());
        }
    }

    public void locationAndWifiOn(Activity activity) {
        //Check that location services are enabled
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(activity, "Turn Location on to save geolocation data.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            activity.startActivity(intent);
        }

        WifiManager wifiManager = (WifiManager) activity.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(activity, "Turn Wifi on to save geolocation data.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            activity.startActivity(intent);
        }
    }

    public Location requestLoc(Location globalLocation) {
        try {
            // Iterate over the providers and their respective listeners
            for (int i = 0; i < providers.size(); i++) { // providers: passive, gps, network //TODO: Switch to attempting to get GPS first, then fall back on the other providers
                Location loc = locationListeners.get(i).getLoc();
                if (loc == null) {
                    // Fall back to getLastKnown (usually less accurate)
                    loc = locationManager.getLastKnownLocation(providers.get(i));
                }
                // Not all providers provide a location at all times (e.g. GPS_PROVIDER doesn't work well indoors). For now, we'll just choose whichever the last working location on in the list is. In the future this could be improved by doing something like Location.getTime(), getBestProvider, or switching entirely over to the getting the location via the Google Play API.
                if (loc != null) {
                    globalLocation = loc;
                }
            }
            return globalLocation;
        } catch (SecurityException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void startListening() {
        try {
            for (int i = 0; i < providers.size(); i++) {
                // Register for location updates using the provider and begin listening for updates
                locationManager.requestLocationUpdates(providers.get(i), 5000, 5, locationListeners.get(i));
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        //  Note: Listening for a long time drains the battery. For now, we'll assume that the listening stops when the user navigates away from the activity. If not, a potential fix is to call locationManager.removeUpdates(locationListener) during from the activity's onPause() function.
    }

}
