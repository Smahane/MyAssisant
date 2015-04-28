package com.phonesettings.myassistant.utils;

import com.example.myassistant.R;

public class C {

	// Singleton
	private static C instance;

	public static synchronized C getInstance() {
		if (instance == null) {
			instance = new C();
		}
		return instance;

	}

	public static class prefs {
		public static final String PREFS_NAME = "SetThingsPreferences";
		public static final String show_volume = "volumeShow";
		public static final String default_radius = "defaultRadius";
		public static final String checking_time = "checkingTime";
		public static final String radius_unit = "radiusUnit";
		public static final String not_exit_from_app = "is_app_running";
	}

	public static final int EARTH_RADIUS_IN_KM = 6371;
	public static final double FEET_TO_METER_CONSTANT = 0.3048;

	public static int CHECK_TIME_IN_MINUTES = 3;
	public static int RADIUS_IN_METERS = 100;

	public static final int NOTIFICATION_ID = 132;
	public static final int EDIT_SITUATION = 1000;
	public static final int ADD_SITUATION = 1001;
	public static final int ADD_SETTING = 1002;

	public static final int BLUETOOTH_REQUEST = 1003;
	public static final int RINGTONE_REQUEST = 1004;
	public static final int BRIGHTNESS_REQUEST = 1005;
	public static final int VOLUME_REQUEST = 1006;
	public static final int WALLPAPER_REQUEST = 1007;
	// public static final int LIVE_WALLPAPER_REQUEST = 1008;

	public static final int BATTERY_REQUEST = 1009;
	public static final int ORIENTATION_REQUEST = 1010;
	public static final int TIME_REQUEST = 1011;
	public static final int LOCATION_REQUEST = 1012;
	public static final int CONTACT_REQUEST = 1013;

	public int getIconIdByTitle(int titleId) {
		switch (titleId) {
		case R.string.contact:
			return R.drawable.blank;
		case R.string.location:
			return R.drawable.blank;
		case R.string.time:
			return R.drawable.blank;
		/*case R.string.bluetooth:
			return R.drawable.bluetooth;
		case R.string.brightness:
			return R.drawable.brightness_full;*/
		case R.string.ringtone:
			return R.drawable.blank;
		/*case R.string.screen_timeout:
			return R.drawable.screen_timeout;*/
		case R.string.volume:
			return R.drawable.blank;
		/*case R.string.wallpaper:
			return R.drawable.wallpaper;
		case R.string.wi_fi:
			return R.drawable.wi_fi;*/
		}
		return -1;
	}
	/*
	 * private boolean isMyServiceRunning() { ActivityManager manager =
	 * (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE); for
	 * (RunningServiceInfo service :
	 * manager.getRunningServices(Integer.MAX_VALUE)) { if
	 * ("com.example.MyService".equals(service.service.getClassName())) { return
	 * true; } } return false; }
	 */

}
