package com.phonesettings.myassistant.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

	// TABELS
	public static class TABLE_SITUATION {
		public static final String SQL_TABLE_NAME = "situations";
		public static final String ID = "_id";
		public static final String IS_ACTIVE = "is_active";// is enabled
		public static final String SITUATION_NAME = "name";
		public static final String POSITION = "position";
		public static final String RUN_STATUS = "is_running";// running in the
																// moment
	}

	public static class TABLE_CONDITION {
		public static final String SQL_TABLE_NAME = "conditions";
		public static final String ID = "id";
		public static final String SITUATION_ID = "situation_id";
		public static final String TITLE = "condition_title";
		public static final String DESCRIPTION = "condition_desc";
		public static final String NOTE = "condition_note";
	}

	public static class TABLE_SETTING {
		public static final String SQL_TABLE_NAME = "settings";
		public static final String ID = "id";
		public static final String SITUATION_ID = "situation_id";
		public static final String TITLE = "setting_title";
		public static final String DESCRIPTION = "setting_desc";
		public static final String NOTE = "setting_note";
	}

	public static class TABLE_TIME_ALARM {
		public static final String SQL_TABLE_NAME = "alarms";
		public static final String ID = "id";
		public static final String SITUATION_ID = "situation_id";
		public static final String STARTING_HOUR = "hour";
		public static final String STARTING_MINUTE = "minute";
		public static final String REPEATING_DAY = "repeat";
	}

	public static class TABLE_LOCATION {
		public static final String SQL_TABLE_NAME = "locations";
		public static final String ID = "id";
		public static final String SITUATION_ID = "situationId";
		public static final String LATITUDE = "lat";
		public static final String LONGITUDE = "lng";
		public static final String RADIUS = "radius";
		public static final String ADDRESS = "address";
		public static final String IS_FAVORITE = "favorite"; // 0 = false; 1 =
																// true; for
																// future use
	}

	public static final String SQL_CREATE_TABLE_SITUATIONS = "CREATE TABLE "
			+ TABLE_SITUATION.SQL_TABLE_NAME + "( " + TABLE_SITUATION.ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ TABLE_SITUATION.IS_ACTIVE + " BOOLEAN, "
			+ TABLE_SITUATION.SITUATION_NAME + " VARCHAR(255), "
			+ TABLE_SITUATION.POSITION + " INTEGER, "
			+ TABLE_SITUATION.RUN_STATUS + " BOOLEAN " + ");";

	public static final String SQL_CREATE_TABLE_CONDITIONS = "CREATE TABLE "
			+ TABLE_CONDITION.SQL_TABLE_NAME + "( " + TABLE_CONDITION.ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ TABLE_CONDITION.SITUATION_ID + " INTEGER, "
			+ TABLE_CONDITION.TITLE + " VARCHAR(255), "
			+ TABLE_CONDITION.DESCRIPTION + " VARCHAR(255), "
			+ TABLE_CONDITION.NOTE + " TEXT " + ");";

	public static final String SQL_CREATE_TABLE_SETTINGS = "CREATE TABLE "
			+ TABLE_SETTING.SQL_TABLE_NAME + "( " + TABLE_SETTING.ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ TABLE_SETTING.SITUATION_ID + " INTEGER, " + TABLE_SETTING.TITLE
			+ " INTEGER, " + TABLE_SETTING.DESCRIPTION + " VARCHAR(255), "
			+ TABLE_SETTING.NOTE + " TEXT " + ");";

	public static final String SQL_CREATE_TABLE_ALARMS = "CREATE TABLE "
			+ TABLE_TIME_ALARM.SQL_TABLE_NAME + "( " + TABLE_TIME_ALARM.ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ TABLE_TIME_ALARM.SITUATION_ID + " INTEGER, "
			+ TABLE_TIME_ALARM.STARTING_HOUR + " INTEGER, "
			+ TABLE_TIME_ALARM.STARTING_MINUTE + " INTEGER, "
			+ TABLE_TIME_ALARM.REPEATING_DAY + " INTEGER " + ");";

	public static final String SQL_CREATE_TABLE_LOCATIONS = "CREATE TABLE "
			+ TABLE_LOCATION.SQL_TABLE_NAME + "( " + TABLE_LOCATION.ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ TABLE_LOCATION.SITUATION_ID + " INTEGER, "
			+ TABLE_LOCATION.LATITUDE + " VARCHAR(255), "
			+ TABLE_LOCATION.LONGITUDE + " VARCHAR(255), "
			+ TABLE_LOCATION.RADIUS + " INTEGER, " + TABLE_LOCATION.ADDRESS
			+ " VARCHAR(255), " + TABLE_LOCATION.IS_FAVORITE + " VARCHAR(255)"
			+ ");";

	public static final String DB_NAME = "SetThings.db";
	public static int DB_VERSION = 1;

	public DatabaseHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, DB_NAME, factory, DB_VERSION);
		// TODO Auto-generated constructor stub
	}

	public DatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DatabaseHelper.SQL_CREATE_TABLE_SITUATIONS);
		db.execSQL(DatabaseHelper.SQL_CREATE_TABLE_CONDITIONS);
		db.execSQL(DatabaseHelper.SQL_CREATE_TABLE_SETTINGS);
		db.execSQL(DatabaseHelper.SQL_CREATE_TABLE_ALARMS);
		db.execSQL(DatabaseHelper.SQL_CREATE_TABLE_LOCATIONS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (newVersion > DB_VERSION) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_SITUATION.SQL_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONDITION.SQL_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTING.SQL_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS "
					+ TABLE_TIME_ALARM.SQL_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCATION.SQL_TABLE_NAME);
			onCreate(db);
			DB_VERSION = newVersion;
		}
	}
}