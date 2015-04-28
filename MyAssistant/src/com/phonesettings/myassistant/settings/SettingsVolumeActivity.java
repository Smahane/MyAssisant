package com.phonesettings.myassistant.settings;

import com.example.myassistant.R;
import com.phonesettings.myassistant.db.SettingManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings.SettingNotFoundException;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class SettingsVolumeActivity extends Activity {
    private TextView percentRing;
    private TextView percentNotification;
    private CheckBox checkBoxRing;
    private CheckBox checkBoxNotification;

    private SettingManager settingsManager;
    private boolean isUpdate;
    private String title;
    private long sitId;

    private int notifMax;
    private int ringMax;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_volume);

        final SeekBar mRingtoneProgress = (SeekBar) findViewById(R.id.seekbar_ringtone_volume);
        final SeekBar mNotificationProgress = (SeekBar) findViewById(R.id.seekbar_notification_volume);
        percentRing = (TextView) findViewById(R.id.ringtone_volume_percent);
        percentNotification = (TextView) findViewById(R.id.notification_volume_percent);
        checkBoxRing = (CheckBox) findViewById(R.id.checkbox_ringtone_vibrate);
        checkBoxNotification = (CheckBox) findViewById(R.id.checkbox_notification_vibrate);

        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        notifMax = am.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
        ringMax = am.getStreamMaxVolume(AudioManager.STREAM_RING);
        mRingtoneProgress.setMax(ringMax);
        mNotificationProgress.setMax(notifMax);

        int notificValue = 5;
        int ringValue = 5;
        boolean ringVbrOn = true;
        boolean notificVbrOn = true;

        settingsManager = new SettingManager(SettingsVolumeActivity.this);
        Intent intent = getIntent();
        sitId = intent.getLongExtra("situationId", -1);
        title = getResources().getResourceEntryName(R.string.volume);
        isUpdate = settingsManager.hasThisSetting(title, sitId);

        if(isUpdate){
            String note = settingsManager.getNote(title, sitId);
            String data[]=note.split(";");

            String ring[]=data[0].split(" ");
            ringValue = Integer.valueOf(ring[0]);
            ringVbrOn = (ring[1].equals("On"))?true:false;

            String notific[]=data[1].split(" ");
            notificValue = Integer.valueOf(notific[0]);
            notificVbrOn = (notific[1].equals("On"))?true:false;
        }else
            try {
                //return between 0 and 7
                ringValue = android.provider.Settings.System.getInt(
                        getContentResolver(),
                        android.provider.Settings.System.VOLUME_RING);


                notificValue = android.provider.Settings.System.getInt(
                        getContentResolver(),
                        android.provider.Settings.System.VOLUME_NOTIFICATION);

            } catch (SettingNotFoundException e) {
                e.printStackTrace();
            }

        mRingtoneProgress.setProgress(ringValue);
        percentRing.setText(String.valueOf(ringValue));
        mNotificationProgress.setProgress(notificValue);
        percentNotification.setText(String.valueOf(notificValue));
        checkBoxRing.setChecked(ringVbrOn);
        checkBoxNotification.setChecked(notificVbrOn);

        mRingtoneProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {
                String p = String.valueOf(progress);
                percentRing.setText(p);
                // Set volume
                /*android.provider.Settings.System.putInt(getContentResolver(),
						android.provider.Settings.System.VOLUME_RING,
						(int)( progress));*/
            }
        });


        mNotificationProgress.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                    boolean fromUser) {

                String p = String.valueOf(progress);
                percentNotification.setText(p);
            }
        });

        checkBoxRing.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });

        checkBoxNotification.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            }
        });

    }

    @Override
    public void onBackPressed() {

        String ringPerc = String.valueOf(percentRing.getText().toString().charAt(0));
        boolean ringVibrate = checkBoxRing.isChecked();

        String notificPerc = String.valueOf(percentNotification.getText().toString().charAt(0));
        boolean notificVibrate = checkBoxNotification.isChecked();

        String note = ringPerc;
        if(ringVibrate){
            note = note.concat(" On;");
        }else{
            note = note.concat(" Off;");
        }
        note = note.concat(notificPerc);
        if(notificVibrate){
            note = note.concat(" On");
        }else{
            note = note.concat(" Off");
        }

        String desc = getString(R.string.ringtone).
                concat(": ").
                concat(ringPerc).
                concat("/").
                concat(String.valueOf(ringMax)).
                concat("; ").
                concat(getString(R.string.notification)).
                concat(": ").
                concat(notificPerc).
                concat("/").
                concat(String.valueOf(notifMax));

        if(isUpdate){
            settingsManager.updateSetting(title, sitId, desc, note);
        }else{
            settingsManager.addSetting(title, sitId, desc, note);
        }

        super.onBackPressed();
    }


    @Override
    protected void onDestroy() {
        settingsManager.stop();
        super.onDestroy();
    }


}
