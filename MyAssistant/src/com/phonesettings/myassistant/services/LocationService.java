package com.phonesettings.myassistant.services;

import com.phonesettings.myassistant.utils.C;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class LocationService extends Service{
    private final String TAG = "LocationService";
    private static boolean isRunning=false;// if service is running
    // An alarm for rising in special times to fire the pendingIntentPositioning
    private AlarmManager alarmManagerPositioning;
    // A PendingIntent for calling a receiver in special times
    public PendingIntent pendingIntentPositioning;


    @Override
    public void onCreate() {
        super.onCreate();

        alarmManagerPositioning = (AlarmManager) getSystemService
                (Context.ALARM_SERVICE);

        Intent intentToFire = new Intent(
                LocationReceiver.ACTION_REFRESH_SCHEDULE_ALARM);
        
        pendingIntentPositioning = PendingIntent.getBroadcast(
                this, 0, intentToFire, 0);
        
        Log.i("SERVICE","service started");
        isRunning = true;
    };

    @Override
    public void onStart(Intent intent, int startId) {
        try {
            // repeating refresh every 3 minutes
            long REPEAT_INTERVAL_LOCATION = C.CHECK_TIME_IN_MINUTES * 60 * 1000;
            int alarmType = AlarmManager.RTC_WAKEUP;
            long timetoRefresh = System.currentTimeMillis();
            alarmManagerPositioning.setRepeating(alarmType,
                    timetoRefresh, REPEAT_INTERVAL_LOCATION, pendingIntentPositioning);
            Log.i("SERVICE","alarm started");
        } catch (NumberFormatException e) {
            Log.e(TAG, "error running service: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "error running service: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {
        this.alarmManagerPositioning.cancel(pendingIntentPositioning);
        LocationReceiver.stopLocationListener();
        Log.e("SERVICE", "service destroyed");
        isRunning = false;
    }

    public static boolean isRunning(){
        return isRunning;
    }

}
