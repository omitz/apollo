package apollo_utils;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

// https://stackoverflow.com/questions/1513485/how-do-i-get-the-current-gps-location-programmatically-in-android

public class ApolloLocationListener implements LocationListener {

    private Location loc;

    public Location getLoc() {
        return loc;
    }

    @Override
    public void onLocationChanged(Location loc) {
        String longitude = "Longitude: " + loc.getLongitude();
        String latitude = "Latitude: " + loc.getLatitude();
        this.loc = loc;
    }

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

}