package com.phonesettings.myassistant.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_CONDITION;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_SETTING;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_SITUATION;

public class SettingManager {
	private final DatabaseHelper helper;
	SQLiteDatabase db;

	public SettingManager(Context c) {
		helper = new DatabaseHelper(c);
		db = helper.getWritableDatabase();
	}

	public long addSetting(String title, long situationId, String description,
			String note) {
		ContentValues values = new ContentValues();
		values.put(TABLE_SETTING.SITUATION_ID, situationId);
		values.put(TABLE_SETTING.TITLE, title);
		values.put(TABLE_SETTING.DESCRIPTION, description);
		values.put(TABLE_SETTING.NOTE, note);

		// Inserting Row
		long rowID = db.insert(TABLE_SETTING.SQL_TABLE_NAME, null, values);

		return rowID;
	}

	public Cursor getAllSettingsForSituation(long situationId) {
		return db.query(TABLE_SETTING.SQL_TABLE_NAME, new String[] {
				TABLE_SETTING.ID + " AS _id", TABLE_SETTING.SITUATION_ID,
				TABLE_SETTING.TITLE, TABLE_SETTING.DESCRIPTION },
				TABLE_SETTING.SITUATION_ID + " = " + situationId, null, null,
				null, TABLE_SETTING.TITLE + " ASC");
	}

	public void updateSetting(String settingName, long situationId,
			String description, String note) {
		ContentValues values = new ContentValues();
		values.put(TABLE_SETTING.DESCRIPTION, description);
		values.put(TABLE_SETTING.NOTE, note);

		db.update(TABLE_SETTING.SQL_TABLE_NAME, values, TABLE_SETTING.TITLE
				+ " = '" + settingName + "' AND " + TABLE_SETTING.SITUATION_ID
				+ " = " + situationId, null);
	}

	public void deleteSetting(String name, long situationId) {
		db.delete(TABLE_SETTING.SQL_TABLE_NAME, TABLE_SETTING.TITLE + " = '"
				+ name + "' AND " + TABLE_SETTING.SITUATION_ID + " = "
				+ situationId, null);
	}

	public Cursor getSettingsByCondition(String conditionTitle) {
		return db.rawQuery("SELECT * FROM " + TABLE_SETTING.SQL_TABLE_NAME
				+ ", " + TABLE_CONDITION.SQL_TABLE_NAME + ", "
				+ TABLE_SITUATION.SQL_TABLE_NAME + " WHERE "
				+ TABLE_CONDITION.SQL_TABLE_NAME + "." + TABLE_CONDITION.TITLE
				+ "='" + conditionTitle + "' AND "
				+ TABLE_CONDITION.SQL_TABLE_NAME + "."
				+ TABLE_CONDITION.SITUATION_ID + "="
				+ TABLE_SETTING.SQL_TABLE_NAME + "."
				+ TABLE_SETTING.SITUATION_ID + " AND "
				+ TABLE_SETTING.SQL_TABLE_NAME + "."
				+ TABLE_SETTING.SITUATION_ID + "="
				+ TABLE_SITUATION.SQL_TABLE_NAME + "." + TABLE_SITUATION.ID
				+ " AND " + TABLE_SITUATION.SQL_TABLE_NAME + "."
				+ TABLE_SITUATION.IS_ACTIVE + "=" + "\"true\" "// TO FIX:=1
				+ "ORDER BY " + TABLE_SITUATION.POSITION + " DESC", null);
	}

	public String getNote(String name, long situationId) {
		Cursor c = db.query(TABLE_SETTING.SQL_TABLE_NAME,
				new String[] { TABLE_SETTING.NOTE }, TABLE_SETTING.TITLE
						+ " = '" + name + "' AND " + TABLE_SETTING.SITUATION_ID
						+ " = " + situationId, null, null, null, null);
		c.moveToFirst();
		String note = c.getString(c.getColumnIndex(TABLE_SETTING.NOTE));
		c.close();

		return note;
	}

	public boolean hasThisSetting(String title, long situationId) {
		Cursor cur = db.query(TABLE_SETTING.SQL_TABLE_NAME,
				new String[] { TABLE_SETTING.TITLE + " AS _id " },
				TABLE_SETTING.TITLE + " = '" + title + "' AND "
						+ TABLE_SETTING.SITUATION_ID + " = " + situationId,
				null, null, null, null);
		boolean answer = false;
		if (cur.getCount() > 0) {
			answer = true;
		}
		cur.close();
		return answer;
	}

	public void stop() {
		helper.close();
		db.close();
	}
}