package com.phonesettings.myassistant.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_TIME_ALARM;

public class TimeAlarmManager {
	private DatabaseHelper helper;
	private SQLiteDatabase db;

	public TimeAlarmManager(Context c) {
		helper = new DatabaseHelper(c);
		db = helper.getWritableDatabase();
	}

	public long addNewAlarm(long situationId, int startHour, int startMinute,
			int repeat) {
		ContentValues values = new ContentValues();
		values.put(TABLE_TIME_ALARM.SITUATION_ID, situationId);
		values.put(TABLE_TIME_ALARM.STARTING_HOUR, startHour);
		values.put(TABLE_TIME_ALARM.STARTING_MINUTE, startMinute);
		values.put(TABLE_TIME_ALARM.REPEATING_DAY, repeat);

		// Inserting Row
		long rowID = db.insert(TABLE_TIME_ALARM.SQL_TABLE_NAME, null, values);

		return rowID;
	}

	public Cursor getAllAlarms() {
		return db.query(TABLE_TIME_ALARM.SQL_TABLE_NAME, new String[] {
				TABLE_TIME_ALARM.ID + " AS _id", TABLE_TIME_ALARM.SITUATION_ID,
				TABLE_TIME_ALARM.STARTING_HOUR,
				TABLE_TIME_ALARM.STARTING_MINUTE,
				TABLE_TIME_ALARM.REPEATING_DAY }, null, null, null, null, null);
	}

	public Cursor getAllAlarmsForSituation(long situationId) {
		return db.query(TABLE_TIME_ALARM.SQL_TABLE_NAME, new String[] {
				TABLE_TIME_ALARM.ID /* + " AS _id" */,
				TABLE_TIME_ALARM.SITUATION_ID, TABLE_TIME_ALARM.STARTING_HOUR,
				TABLE_TIME_ALARM.STARTING_MINUTE,
				TABLE_TIME_ALARM.REPEATING_DAY }, TABLE_TIME_ALARM.SITUATION_ID
				+ " = " + situationId, null, null, null, null);
	}

	// return 2 IDs: 1 for start and one for end
	public Cursor getId(long situationId, int day) {
		return db.query(TABLE_TIME_ALARM.SQL_TABLE_NAME, new String[] {
				TABLE_TIME_ALARM.ID + " AS _id", TABLE_TIME_ALARM.SITUATION_ID,
				TABLE_TIME_ALARM.STARTING_HOUR,
				TABLE_TIME_ALARM.STARTING_MINUTE,
				TABLE_TIME_ALARM.REPEATING_DAY }, TABLE_TIME_ALARM.SITUATION_ID
				+ " = " + situationId + " AND "
				+ TABLE_TIME_ALARM.REPEATING_DAY + " = " + day, null, null,
				null, null);
	}

	public void deleteAlarm(long situationId, int repeat) {
		db.delete(TABLE_TIME_ALARM.SQL_TABLE_NAME,
				TABLE_TIME_ALARM.SITUATION_ID + " = " + situationId + " AND "
						+ TABLE_TIME_ALARM.REPEATING_DAY + " = " + repeat, null);
	}

	public void deleteAllAlarmsForSituation(long situationId) {
		db.delete(TABLE_TIME_ALARM.SQL_TABLE_NAME,
				TABLE_TIME_ALARM.SITUATION_ID + " = " + situationId, null);
	}

	public void stop() {
		helper.close();
		db.close();
	}
}