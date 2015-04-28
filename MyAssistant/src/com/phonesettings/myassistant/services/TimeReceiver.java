package com.phonesettings.myassistant.services;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import com.phonesettings.myassistant.SettingsMaker;
import com.phonesettings.myassistant.conditions.ConditionsTimeActivity;
import com.phonesettings.myassistant.db.DatabaseHelper;
import com.phonesettings.myassistant.db.TimeAlarmManager;

public class TimeReceiver extends BroadcastReceiver {

    public static final String SET_ALARM = "net.devstudio.setthings.services.SET_ALARMS";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("TimeReceiver", "Received: "+intent.getAction());
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            // restoring all alarms
            TimeAlarmManager timeAlarmManager = new TimeAlarmManager(context);
            Cursor c = timeAlarmManager.getAllAlarms();
            int sitIdIndex = c.getColumnIndex( DatabaseHelper.TABLE_TIME_ALARM.SITUATION_ID );
            int startHourIndex = c.getColumnIndex( DatabaseHelper.TABLE_TIME_ALARM.STARTING_HOUR );
            int startMinuteIndex = c.getColumnIndex( DatabaseHelper.TABLE_TIME_ALARM.STARTING_MINUTE );
            int repeatIndex = c.getColumnIndex( DatabaseHelper.TABLE_TIME_ALARM.REPEATING_DAY );

            while(c.moveToNext()){
                long sitId = c.getLong(sitIdIndex);
                int repDay = c.getInt(repeatIndex);
                int startHour = c.getInt(startHourIndex);
                int startMin = c.getInt(startMinuteIndex);

                Intent alarmIntent = new Intent(TimeReceiver.SET_ALARM);
                PendingIntent sender = PendingIntent.getBroadcast(context,(int) sitId, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                Calendar timeOff = Calendar.getInstance();
                int currDay = timeOff.get(Calendar.DAY_OF_WEEK);
                int days=ConditionsTimeActivity.getDaysDifference(currDay, repDay);

                timeOff.add(Calendar.DAY_OF_WEEK, days);
                timeOff.set(Calendar.HOUR_OF_DAY, startHour);
                timeOff.set(Calendar.MINUTE, startMin);
                timeOff.set(Calendar.SECOND, 0);
                alarm.setRepeating(AlarmManager.RTC_WAKEUP, timeOff.getTimeInMillis(), 7 * AlarmManager.INTERVAL_DAY, sender);
            }
            c.close();
            timeAlarmManager.stop();
        }else if(intent.getAction().equals(TimeReceiver.SET_ALARM)){
            Log.e("RECEIVER-TIME", "ALARM STARTED!");
            //SettingsMaker.getInstance(context.getApplicationContext()).update();

            Intent service = new Intent(context.getApplicationContext(), SettingsMaker.class);
            context.startService(service);

        }
    }

}
