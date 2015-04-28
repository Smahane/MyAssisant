package com.phonesettings.myassistant;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.location.Location;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import com.example.myassistant.R;
import com.phonesettings.myassistant.db.ConditionManager;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_CONDITION;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_LOCATION;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_SETTING;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_SITUATION;
import com.phonesettings.myassistant.db.MarkerManager;
import com.phonesettings.myassistant.db.SettingManager;
import com.phonesettings.myassistant.db.SituationManager;
import com.phonesettings.myassistant.utils.C;

public class SettingsMaker extends Service {
	static Context context;

	public static int batteryValue = -1;

	public static String contactValue = "a";

	public static Location locationValue = null;

	public static String orientationValue = "";

	// settings values to be set on the end of the check
	private boolean bluetooth = false;

	private String brightness = "50";

	private String ringtone = "";

	private int timeout = 1000;

	private int[] ring = new int[] {};

	private int[] notification = new int[] {};

	private String wallpaper = "";

	private boolean wifi = false;

	private boolean bluetoothChaned = false;

	private boolean brightnessChanged = false;

	private boolean ringtoneChanged = false;

	private boolean timeoutChanged = false;

	private boolean volumeChanged = false;

	private boolean wallpaperChanged = false;

	private boolean wifiChanged = false;

	private boolean mShowVolumeGlobalSetting;

	SharedPreferences pre_pref;

	String cr, cn;
	int v_mode, r_v, n_v;
	AudioManager am;

	public void update() {
		// load global settings:
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				C.prefs.PREFS_NAME, MODE_PRIVATE);
		mShowVolumeGlobalSetting = sharedPreferences.getBoolean(
				C.prefs.show_volume, false);

		pre_pref = getSharedPreferences("presets", MODE_WORLD_READABLE);

		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		v_mode = pre_pref.getInt("v_mode", 0);
		Log.e("v_mode", "" + v_mode);

		r_v = pre_pref.getInt("r_v", 0);
		Log.e("r_v", "" + r_v);

		n_v = pre_pref.getInt("n_v", 0);
		Log.e("n_v", "" + n_v);

		cr = pre_pref.getString("cr_uri", "");
		Log.e("cr", cr);

		cn = pre_pref.getString("cn_uri", "");
		Log.e("cn", "" + cn);

		SituationManager situationManager = new SituationManager(context);
		SettingManager sm = new SettingManager(context);
		ConditionManager cm = new ConditionManager(context);
		Resources r = context.getResources();
		Log.i("SettingsMaker", "UPDATE");
		Cursor cursorSit = situationManager.getAllSituations(true);

		// Start or stop location service
		/*
		 * if (containThisCondition(context,
		 * r.getResourceEntryName(R.string.location), cursorSit)) { // starting
		 * service if necessary if (!LocationService.isRunning()) {
		 * startService(new Intent(this, LocationService.class)); } } else if
		 * (LocationService.isRunning()) { stopService(new Intent(this,
		 * LocationService.class)); }
		 */
		// Start or stop orientation service
		/*if (containThisCondition(context,
				r.getResourceEntryName(R.string.orientation), cursorSit)) {
			// starting service if necessary
			if (!OrientationService.isRunning()) {
				startService(new Intent(this, OrientationService.class));
			}
		} else if (OrientationService.isRunning()) {
			stopService(new Intent(this, OrientationService.class));
		}*/

		// move through every situation
		for (cursorSit.moveToLast(); !cursorSit.isBeforeFirst(); cursorSit
				.moveToPrevious()) {
			/*
			 * String situationName = cursorSit.getString(cursorSit
			 * .getColumnIndex(TABLE_SITUATION.SITUATION_NAME));
			 */
			long sitId = cursorSit.getLong(cursorSit
					.getColumnIndex(TABLE_SITUATION.ID));
			int cond = 0;
			Cursor cursorCond = cm.getAllConditionsForSituation(sitId);
			// check if all conditions are completed
			for (cursorCond.moveToFirst(); !cursorCond.isAfterLast(); cursorCond
					.moveToNext()) {
				String condTitle = cursorCond.getString(cursorCond
						.getColumnIndex(TABLE_CONDITION.TITLE));

				 if (condTitle.equals(r
						.getResourceEntryName(R.string.contact))) {
					if (contactCompleted(sitId))
						cond++;
					else
						break;
				} else if (condTitle.equals(r
						.getResourceEntryName(R.string.location))) {
					if (locationCompleted(sitId))
						cond++;
					else
						break;
				} else if (condTitle.equals(r
						.getResourceEntryName(R.string.time))) {
					if (timeCompleted(sitId))
						cond++;
					else
						break;
				}
			}

			// if all conditions are completed
			if (cond == cursorCond.getCount()) {
				situationManager.updateSituationRunStatus(true, sitId);
				Cursor cursorSet = sm.getAllSettingsForSituation(sitId);

				// get all settings for this situation
				for (cursorSet.moveToFirst(); !cursorSet.isAfterLast(); cursorSet
						.moveToNext()) {
					String settingTitle = cursorSet.getString(cursorSet
							.getColumnIndex(TABLE_SETTING.TITLE));

					if (settingTitle.equals(r
							.getResourceEntryName(R.string.bluetooth))) {
						bluetooth = getBluetooth(sitId);
						bluetoothChaned = true;
					} else if (settingTitle.equals(r
							.getResourceEntryName(R.string.brightness))) {
						brightness = getBrightness(sitId);
						brightnessChanged = true;
					} else if (settingTitle.equals(r
							.getResourceEntryName(R.string.ringtone))) {
						ringtone = getRingtone(sitId);
						ringtoneChanged = true;
					} else if (settingTitle.equals(r
							.getResourceEntryName(R.string.screen_timeout))) {
						timeout = getScreenTimeout(sitId);
						timeoutChanged = true;
					} else if (settingTitle.equals(r
							.getResourceEntryName(R.string.volume))) {
						ring = getRingVolume(sitId);
						notification = getNotificationVolume(sitId);
						volumeChanged = true;
					} else if (settingTitle.equals(r
							.getResourceEntryName(R.string.wallpaper))) {
						wallpaper = getWallpaper(sitId);
						wallpaperChanged = true;
					} else if (settingTitle.equals(r
							.getResourceEntryName(R.string.wi_fi))) {
						wifi = getWiFi(sitId);
						wifiChanged = true;
					}
				}
				cursorSet.close();
			} else {
				situationManager.updateSituationRunStatus(false, sitId);
			}
			cursorCond.close();
		}

		cursorSit.close();

		cm.stop();
		sm.stop();
		situationManager.stop();

		// set the high priority settings
		if (bluetoothChaned) {
			setBluetooth(bluetooth);
			bluetoothChaned = false;
		}
		if (brightnessChanged) {
			//setBrightness(brightness);
			brightnessChanged = false;
		}
		if (ringtoneChanged) {
			setRingtone(ringtone);
			ringtoneChanged = false;
		}
		if (timeoutChanged) {
			setScreenTimeout(timeout);
			timeoutChanged = false;
		}
		if (volumeChanged) {
			setRingVolume(ring);
			setNotificationVolume(notification);
			volumeChanged = false;
		}
		if (wallpaperChanged) {
			//setWallpaper(wallpaper);
			wallpaperChanged = false;
		}
		if (wifiChanged) {
			setWiFi(wifi);
			wifiChanged = false;
		}

		// send broadcast to refresh UI for running situations
		Intent i = new Intent("android.intent.action.MAIN");
		sendBroadcast(i);
	}

	// Conditions
	/*boolean batteryCompleted(long sitId) {
		ConditionManager cm = new ConditionManager(
				context.getApplicationContext());
		String name = context.getResources().getResourceEntryName(
				R.string.battery);
		String data = cm.getNote(name, sitId);
		cm.stop();

		int start = data.indexOf(" ");
		int end = data.indexOf(" %");

		String perc = data.substring(start + 1, end);
		int percent = Integer.valueOf(perc);

		if (data.contains("<")) {
			return (batteryValue < percent);
		} else if (data.contains(">")) {
			return (batteryValue > percent);
		}
		return false;
	}*/

	boolean contactCompleted(long situationId) {
		ConditionManager cm = new ConditionManager(
				context.getApplicationContext());
		String name = context.getResources().getResourceEntryName(
				R.string.contact);
		String note = cm.getNote(name, situationId);
		cm.stop();

		ArrayList<String> contactNumber = new ArrayList<String>();
		Cursor phones = getContentResolver().query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				null,
				ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = "
						+ note, null, null);
		while (phones.moveToNext()) {
			contactNumber
					.add(phones.getString(phones
							.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)));
		}
		phones.close();

		for (int i = 0; i < contactNumber.size(); i++) {
			if (PhoneNumberUtils.compare(contactNumber.get(i), contactValue)) {
				return true;
			}
		}
		return false;
	}

	
	//ToDo: Complete MarkerManager
	boolean locationCompleted(long situationId) {
		if (locationValue == null) {
			return false;
		}
		float accuracy = locationValue.getAccuracy();

		MarkerManager manager = new MarkerManager(context);
		Cursor cur = manager.getAllLocationsForSituation(situationId);
		int indexLatitude = cur.getColumnIndex(TABLE_LOCATION.LATITUDE);
		int indexLongitude = cur.getColumnIndex(TABLE_LOCATION.LONGITUDE);
		// int indexSituationId =
		// cur.getColumnIndex(TABLE_LOCATION.SITUATION_ID);
		int indexRadius = cur.getColumnIndex(TABLE_LOCATION.RADIUS);
		for (cur.moveToFirst(); !cur.isAfterLast(); cur.moveToNext()) {
			String lat = cur.getString(indexLatitude);
			String lon = cur.getString(indexLongitude);
			String r = cur.getString(indexRadius);
			// long sitId = cur.getInt(indexSituationId);
			int radius = Integer.parseInt(r);
			double latitude = Double.parseDouble(lat);
			double longitude = Double.parseDouble(lon);

			Location dest = new Location("");
			dest.setLatitude(latitude);
			dest.setLongitude(longitude);

			float distance = locationValue.distanceTo(dest);
			if (distance <= radius + accuracy) {
				cur.close();
				manager.stop();
				return true;
			} else {
				setRingtonePreset();
				setNotificationPreset();
			}
		}
		cur.close();
		manager.stop();
		// Toast.makeText(context,
		// "location distance = "+distance+"; acc="+accuracy,
		// Toast.LENGTH_SHORT).show();
		// return distance <= distanceRadius + accuracy;
		return false;
	}

	/*boolean orientationCompleted(long sitId) {
		ConditionManager cm = new ConditionManager(
				context.getApplicationContext());
		String name = context.getResources().getResourceEntryName(
				R.string.orientation);
		String data = cm.getNote(name, sitId);
		cm.stop();

		return data.equals(orientationValue);
	}*/

	boolean timeCompleted(long sitId) {
		ConditionManager cm = new ConditionManager(
				context.getApplicationContext());
		String name = context.getResources()
				.getResourceEntryName(R.string.time);
		String note = cm.getNote(name, sitId);
		cm.stop();

		String data[] = note.split("#");
		String startData[] = data[0].split(":");
		String endData[] = data[1].split(":");
		int mStartHour = Integer.valueOf(startData[0]);
		int mStartMinute = Integer.valueOf(startData[1]);
		int mEndHour = Integer.valueOf(endData[0]);
		int mEndMinute = Integer.valueOf(endData[1]);
		boolean checkedDays[] = { false, false, false, false, false, false,
				false };

		for (int i = 2; i < data.length; i++) {
			checkedDays[Integer.valueOf(data[i])] = true;
		}

		// get current time
		Calendar calendar = Calendar.getInstance();
		int day = calendar.get(Calendar.DAY_OF_WEEK);
		int minutes = calendar.get(Calendar.MINUTE);
		int hour = calendar.get(Calendar.HOUR_OF_DAY);

		// first day of week is Monday = 2
		if (day == Calendar.SUNDAY)
			day = 8;

		int currentTimeInMinutes = hour * 60 + minutes;
		int startingTimeInMinutes = mStartHour * 60 + mStartMinute;
		int endingTimeInMinutes = mEndHour * 60 + mEndMinute;

		if (checkedDays[day - 2] == true) {
			if (startingTimeInMinutes <= currentTimeInMinutes
					&& currentTimeInMinutes < endingTimeInMinutes) {
				return true;
			} else {
				setRingtonePreset();
				setNotificationPreset();
				return false;
			}
		}
		return false;
	}

	// Settings
	public boolean getBluetooth(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.bluetooth);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);
		sm.stop();

		if (note.equals("On")) {
			return true;
		} else if (note.equals("Off")) {
			return false;
		}
		return false;
	}

	public void setBluetooth(boolean isEnable) {
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		if (isEnable) {
			if (!mBluetoothAdapter.isEnabled()) {
				mBluetoothAdapter.enable();
			}
		} else {
			mBluetoothAdapter.disable();
		}
	}

	public void setBluetoothFormSituation(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.bluetooth);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);
		sm.stop();
		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		if (note.equals("On")) {
			if (!mBluetoothAdapter.isEnabled()) {
				mBluetoothAdapter.enable();
			}
		} else if (note.equals("Off")) {
			mBluetoothAdapter.disable();
		}
	}

	public String getBrightness(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.brightness);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String brightness = sm.getNote(name, situationId);
		sm.stop();

		return brightness;
	}


	

	public String getRingtone(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.ringtone);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);// get uri
		sm.stop();

		Log.e("getUri", note);

		return note;
	}

	public void setRingtone(String note) {
		if (!note.equals("")) {
			RingtoneManager.setActualDefaultRingtoneUri(
					context.getApplicationContext(),
					RingtoneManager.TYPE_RINGTONE, Uri.parse(note));
			Log.e("set uri", note);
		} else {
			RingtoneManager.setActualDefaultRingtoneUri(
					context.getApplicationContext(),
					RingtoneManager.TYPE_RINGTONE, (Uri) null);
		}
	}

	/*
	 * public int setContactRingtone(String ringtoneUri, Uri contactId){
	 * ContentValues values = new ContentValues(); int android_sdk_version =
	 * Integer.parseInt(Build.VERSION.SDK); if (android_sdk_version < 7) {
	 * values.put(People.CUSTOM_RINGTONE, ringtoneUri); return
	 * context.getContentResolver().update(People.CONTENT_URI, values,
	 * People._ID + "=" + contactId, null); } else{
	 * values.put(ContactsContract.Contacts.CUSTOM_RINGTONE,
	 * ringtoneUri.toString()); return
	 * context.getContentResolver().update(ContactsContract
	 * .Contacts.CONTENT_URI, values, ContactsContract.Contacts._ID + "=" +
	 * contactId, null); } }
	 */
	public void setRingtoneFromSituation(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.ringtone);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);// get uri
		sm.stop();

		if (!note.equals("")) {
			RingtoneManager.setActualDefaultRingtoneUri(
					context.getApplicationContext(),
					RingtoneManager.TYPE_RINGTONE, Uri.parse(note));
		} else {
			RingtoneManager.setActualDefaultRingtoneUri(
					context.getApplicationContext(),
					RingtoneManager.TYPE_RINGTONE, (Uri) null);
		}
	}

	public int getScreenTimeout(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.screen_timeout);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);// get time
		sm.stop();

		return Integer.valueOf(note);
	}

	public void setScreenTimeout(int value) {
		Settings.System.putInt(context.getContentResolver(),
				Settings.System.SCREEN_OFF_TIMEOUT, value);
	}

	public void setScreenTimeoutFromSituation(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.screen_timeout);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);// get time
		sm.stop();

		Settings.System.putInt(context.getContentResolver(),
				Settings.System.SCREEN_OFF_TIMEOUT, Integer.valueOf(note));

	}

	public int[] getRingVolume(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.volume);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);// get values
		sm.stop();

		String data[] = note.split(";");
		// ring values:
		String ring[] = data[0].split(" ");
		int ringValue = Integer.valueOf(ring[0]);
		int ringVbrOn = (ring[1].equals("On")) ? 1 : 0;

		int[] result = new int[] { ringValue, ringVbrOn };
		return result;

	}

	@SuppressWarnings("deprecation")
	public void setRingVolume(int[] value) {
		try {
			int currentRingValue = android.provider.Settings.System.getInt(
					getContentResolver(),
					android.provider.Settings.System.VOLUME_RING);
			if (value[0] == currentRingValue) {
				return;
			}
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		android.provider.Settings.System.putString(getContentResolver(),
				android.provider.Settings.System.VOLUME_RING,
				String.valueOf(value[0]));

		int flags = AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;
		if (mShowVolumeGlobalSetting) {
			flags |= AudioManager.FLAG_SHOW_UI;
		}
		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_RING, value[0], flags);

		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, value[1]);
		// value[1]: 1 = On; 0 = Off
	}

	public int[] getNotificationVolume(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.volume);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);// get values
		sm.stop();

		String data[] = note.split(";");
		// notification values:
		String notific[] = data[1].split(" ");
		int notificValue = Integer.valueOf(notific[0]);
		int notificVbrOn = (notific[1].equals("On")) ? 1 : 0;

		int[] result = new int[] { notificValue, notificVbrOn };

		return result;
	}

	public void setNotificationVolume(int[] value) {
		try {
			int currentNotificValue = android.provider.Settings.System.getInt(
					getContentResolver(),
					android.provider.Settings.System.VOLUME_NOTIFICATION);
			if (value[0] == currentNotificValue) {
				return;
			}
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		android.provider.Settings.System.putString(getContentResolver(),
				android.provider.Settings.System.VOLUME_NOTIFICATION,
				String.valueOf(value[0]));

		int flags = AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;
		if (mShowVolumeGlobalSetting) {
			flags |= AudioManager.FLAG_SHOW_UI;
		}
		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, value[0], flags);

		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, value[1]);// value[1]:
		// 1 = On; 0 = Off
	}

	@SuppressWarnings("deprecation")
	public void setVolumeFromSituation(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.volume);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);// get values
		sm.stop();

		String data[] = note.split(";");
		// ring values:
		String ring[] = data[0].split(" ");
		int ringValue = Integer.valueOf(ring[0]);
		boolean ringVbrOn = (ring[1].equals("On")) ? true : false;

		// notification values:
		String notific[] = data[1].split(" ");
		int notificValue = Integer.valueOf(notific[0]);
		boolean notificVbrOn = (notific[1].equals("On")) ? true : false;

		android.provider.Settings.System.putString(getContentResolver(),
				android.provider.Settings.System.VOLUME_NOTIFICATION,
				String.valueOf(notificValue));
		android.provider.Settings.System.putString(getContentResolver(),
				android.provider.Settings.System.VOLUME_RING,
				String.valueOf(ringValue));

		int flags = AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE;
		if (mShowVolumeGlobalSetting) {
			flags |= AudioManager.FLAG_SHOW_UI;
		}

		AudioManager am = (AudioManager) context
				.getSystemService(Context.AUDIO_SERVICE);
		am.setStreamVolume(AudioManager.STREAM_RING, ringValue, flags);

		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,
				ringVbrOn ? AudioManager.VIBRATE_SETTING_ON
						: AudioManager.VIBRATE_SETTING_OFF);

		am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, notificValue,
				flags);

		am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION,
				notificVbrOn ? AudioManager.VIBRATE_SETTING_ON
						: AudioManager.VIBRATE_SETTING_OFF);
	}

	public String getWallpaper(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.wallpaper);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);
		sm.stop();

		return note;
	}



	public boolean getWiFi(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.wi_fi);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);
		sm.stop();

		if (note.equals("On")) {
			return true;
		} else if (note.equals("Off")) {
			return false;
		}
		return false;
	}

	public void setWiFi(boolean isEnable) {
		WifiManager wfm = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (isEnable) {
			if (!wfm.isWifiEnabled())
				wfm.setWifiEnabled(true);
		} else {
			wfm.setWifiEnabled(false);
		}
	}

	public void setWiFiFromSituation(long situationId) {
		String name = context.getResources().getResourceEntryName(
				R.string.wi_fi);
		SettingManager sm = new SettingManager(context.getApplicationContext());
		String note = sm.getNote(name, situationId);
		sm.stop();

		WifiManager wfm = (WifiManager) context
				.getSystemService(Context.WIFI_SERVICE);
		if (note.equals("On") && !wfm.isWifiEnabled()) {
			wfm.setWifiEnabled(true);
		} else if (note.equals("Off")) {
			wfm.setWifiEnabled(false);
		}
	}

	private void createNotification() {
		String text = "";
		SituationManager situationManager = new SituationManager(
				context.getApplicationContext());
		Cursor cursorSit = situationManager.getAllSituations(true);
		for (cursorSit.moveToFirst(); !cursorSit.isAfterLast(); cursorSit
				.moveToNext()) {
			String isRunning = cursorSit.getString(cursorSit
					.getColumnIndex(TABLE_SITUATION.RUN_STATUS));
			if (isRunning.equals("1")) {
				String name = cursorSit.getString(cursorSit
						.getColumnIndex(TABLE_SITUATION.SITUATION_NAME));
				text += name + "; ";
			}
		}
		cursorSit.close();
		situationManager.stop();
		/*
		 * RemoteViews expandedView = new RemoteViews(getPackageName(),
		 * R.layout.custom_notification);
		 * expandedView.setImageViewResource(R.id.notification_image,
		 * R.drawable.ic_launcher);
		 * expandedView.setTextViewText(R.id.notification_title,
		 * "My custom notification title");
		 * expandedView.setTextViewText(R.id.notification_text,
		 * "My custom notification text"); notification.bigContentView =
		 * expandedView;
		 * 
		 * Intent intent = new Intent(context, SettingsMaker.class);
		 * PendingIntent piService = PendingIntent.getService(context, 0,
		 * intent, 0);
		 */
		Notification notification = new Notification(R.drawable.ic_launcher,
				text, System.currentTimeMillis());
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		/*
		 * notification.addAction(R.drawable.ic_launcher, "Off All", piService);
		 * notification.addAction(R.drawable.ic_launcher, "More", piService);
		 * notification.addAction(R.drawable.ic_launcher, "And more",
		 * piService); notification.build();
		 */
		PendingIntent piActivity = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class), 0);
		notification.setLatestEventInfo(this, getString(R.string.app_name)
				+ " - " + getString(R.string.active_situations), text,
				piActivity);

		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(C.NOTIFICATION_ID, notification);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences sp = getSharedPreferences(C.prefs.PREFS_NAME,
				Context.MODE_WORLD_READABLE);
		boolean appIsRunning = sp.getBoolean(C.prefs.not_exit_from_app, false);
		if (appIsRunning) {
			context = getApplicationContext();
			update();

			createNotification();
		}

		stopSelf();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public boolean containThisCondition(Context context, String conditionName,
			Cursor situations) {
		ConditionManager cm = new ConditionManager(context);
		for (situations.moveToFirst(); !situations.isAfterLast(); situations
				.moveToNext()) {
			long sitId = situations.getLong(situations
					.getColumnIndex(TABLE_SITUATION.ID));
			if (cm.hasThisCondition(conditionName, sitId)) {
				cm.stop();
				return true;
			}
		}

		cm.stop();
		return false;
	}

	@SuppressWarnings("deprecation")
	public void setRingtonePreset() {
		if (!cr.equals("null") || !cr.equals("")) {
			RingtoneManager.setActualDefaultRingtoneUri(
					context.getApplicationContext(),
					RingtoneManager.TYPE_RINGTONE, Uri.parse(cr));
		} else {
			RingtoneManager.setActualDefaultRingtoneUri(
					context.getApplicationContext(),
					RingtoneManager.TYPE_RINGTONE, (Uri) null);
		}

		am.setStreamVolume(AudioManager.STREAM_RING, r_v, 0);
		if (v_mode != 0) {
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, 0);
		} else {
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, 1);
		}
	}

	@SuppressWarnings("deprecation")
	public void setNotificationPreset() {
		if (!cn.equals("null") || !cn.equals("")) {
			RingtoneManager.setActualDefaultRingtoneUri(
					context.getApplicationContext(),
					RingtoneManager.TYPE_NOTIFICATION, Uri.parse(cn));
		} else {
			RingtoneManager.setActualDefaultRingtoneUri(
					context.getApplicationContext(),
					RingtoneManager.TYPE_NOTIFICATION, (Uri) null);
		}

		am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, n_v, 0);
		if (v_mode != 0) {
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, 0);
		} else {
			am.setVibrateSetting(AudioManager.VIBRATE_TYPE_NOTIFICATION, 1);
		}
	}
}
