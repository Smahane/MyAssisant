package com.phonesettings.myassistant.services;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.phonesettings.myassistant.SettingsMaker;

public class LocationReceiver extends BroadcastReceiver {
    //public static final int MIN_TIME_REQUEST = 2 * 60 * 1000;

    public static final String ACTION_REFRESH_SCHEDULE_ALARM =
            "net.devstudio.setthings.services.ACTION_REFRESH_SCHEDULE_ALARM";

    private static Location currentLocation;
    private static Location prevLocation;
    private static Context _context;
    private String provider = LocationManager.GPS_PROVIDER;

    private static LocationManager locationManager;
    private static LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            try {
                String strStatus = "";
                switch (status) {
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        strStatus = "GPS_EVENT_FIRST_FIX";
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        strStatus = "GPS_EVENT_SATELLITE_STATUS";
                        break;
                    case GpsStatus.GPS_EVENT_STARTED:
                        strStatus = "GPS_EVENT_STARTED";
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        strStatus = "GPS_EVENT_STOPPED";
                        break;

                    default:
                        strStatus = String.valueOf(status);
                        break;
                }
                /* Toast.makeText(_context,
	                    "Status: " + strStatus,
	                    Toast.LENGTH_SHORT).show();*/
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

        @Override
        public void onLocationChanged(Location location) {
            try {
                /*Toast.makeText(_context,
                        "***new location***",
                        Toast.LENGTH_SHORT).show();*/
                gotLocation(location);
                Log.i("RECEIVER", "location changed!!!");
            } catch (Exception e) {

            }
        }
    };

    // received request from the calling service
    @Override
    public void onReceive(final Context context, Intent intent) {
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            context.startService(new Intent(context, LocationService.class));
        }

        /*Toast.makeText(context, "new request received by receiver",
                Toast.LENGTH_SHORT).show();*/
        _context = context;

        locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_MEDIUM);

        provider = locationManager.getBestProvider(criteria, true);

        locationManager.requestLocationUpdates(provider, 0/*MIN_TIME_REQUEST*/,
                5, locationListener);
        Location gotLoc = locationManager.getLastKnownLocation(provider);
        gotLocation(gotLoc);
        /*Toast.makeText(context, "Best Provider: "+provider,
                Toast.LENGTH_SHORT).show();*/
    }

    private static void gotLocation(Location location) {
        prevLocation = currentLocation == null ?
                null : new Location(currentLocation);
        currentLocation = location;

        if (isLocationNew()) {
            OnNewLocationReceived(location);

            /*Toast.makeText(_context, "new location saved", Toast.LENGTH_SHORT)
            .show();*/
            Log.i("RECEIVER", "New location saved!!!");
            SettingsMaker.locationValue = location;
            Intent service = new Intent(_context.getApplicationContext(), SettingsMaker.class);
            _context.startService(service);// TODO: Go to Receiver, which check only locations

            stopLocationListener();
        }
    }

    private static boolean isLocationNew() {
        if (currentLocation == null) {
            return false;
        } else if (prevLocation == null) {
            return true;
        } else if (currentLocation.getTime() == prevLocation.getTime()) {
            return false;
        } else {
            return true;
        }
    }

    public static void stopLocationListener() {
        if(locationManager!=null){
            locationManager.removeUpdates(locationListener);
        }
        /*Toast.makeText(_context, "provider stoped", Toast.LENGTH_SHORT)
        .show();*/
    }


    // listener ----------------------------------------------------
    static ArrayList<OnNewLocationListener> arrOnNewLocationListener =
            new ArrayList<OnNewLocationListener>();

    // Allows the user to set a OnNewLocationListener outside of this class and
    // react to the event.
    // A sample is provided in ActDocument.java in method: startStopTryGetPoint
    public static void setOnNewLocationListener(
            OnNewLocationListener listener) {
        arrOnNewLocationListener.add(listener);
    }

    public static void clearOnNewLocationListener(
            OnNewLocationListener onNewLocationListener) {
        arrOnNewLocationListener.remove(onNewLocationListener);
    }

    // This function is called after the new point received
    private static void OnNewLocationReceived(Location location) {
        // Check if the Listener was set, otherwise we'll get an Exception when
        // we try to call it
        if (arrOnNewLocationListener != null) {
            // Only trigger the event, when we have any listener
            for (int i = 0; i < arrOnNewLocationListener.size(); i++) {
                arrOnNewLocationListener.get(i).onNewLocationReceived(
                        location);
            }
        }
    }
}