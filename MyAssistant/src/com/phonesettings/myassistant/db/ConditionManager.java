package com.phonesettings.myassistant.db;

import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_CONDITION;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public class ConditionManager {
	private DatabaseHelper helper;
	private SQLiteDatabase db;
	public ConditionManager(Context c){
		helper = new DatabaseHelper(c);	
		db = helper.getWritableDatabase();
	}
	
	public long addCondition(String title, long situationId, String description, String note) {
		ContentValues values = new ContentValues();
		values.put(TABLE_CONDITION.SITUATION_ID, situationId);
		values.put(TABLE_CONDITION.TITLE, title);
		values.put(TABLE_CONDITION.DESCRIPTION, description);
		values.put(TABLE_CONDITION.NOTE, note);

		// Inserting Row
		long rowID = db.insert(TABLE_CONDITION.SQL_TABLE_NAME, null, values);

		return rowID;
	}

	public Cursor getAllConditionsForSituation(long situationId){		
		return db.query(TABLE_CONDITION.SQL_TABLE_NAME, 
				new String[]{
				TABLE_CONDITION.ID + " AS _id", 
				TABLE_CONDITION.SITUATION_ID, 
				TABLE_CONDITION.TITLE,
				TABLE_CONDITION.DESCRIPTION}, TABLE_CONDITION.SITUATION_ID + " = " + situationId, null, null, null, 
				TABLE_CONDITION.TITLE + " ASC");			
	}
	
	public void updateCondition(String conditionName, long situationId, String description, String note){	
		ContentValues values = new ContentValues();
		values.put(TABLE_CONDITION.DESCRIPTION, description);	
		values.put(TABLE_CONDITION.NOTE, note);
		db.update(TABLE_CONDITION.SQL_TABLE_NAME, values, TABLE_CONDITION.TITLE + " = '"+conditionName+"' AND " + TABLE_CONDITION.SITUATION_ID+
				" = "+situationId, null);		
	}
	
	
	public void deleteCondition(String name, long situationId){
		db.delete(TABLE_CONDITION.SQL_TABLE_NAME, TABLE_CONDITION.TITLE + " = '" + name + "' AND "+TABLE_CONDITION.SITUATION_ID+
				" = "+situationId , null);		
	}
	
	public String getDescription(String name, long situationId){
		Cursor c =  db.query(TABLE_CONDITION.SQL_TABLE_NAME, 
				new String[]{
				TABLE_CONDITION.DESCRIPTION}, 
				TABLE_CONDITION.TITLE + " = '" + name + "' AND " + TABLE_CONDITION.SITUATION_ID + " = " + situationId, null, null, null, 
				null);	
		c.moveToFirst();
		String desc = c.getString(c.getColumnIndex(TABLE_CONDITION.DESCRIPTION));
		c.close();
		
		return desc;
	}
	
	public String getNote(String name, long situationId){
		Cursor c =  db.query(TABLE_CONDITION.SQL_TABLE_NAME, 
				new String[]{
				TABLE_CONDITION.NOTE}, 
				TABLE_CONDITION.TITLE + " = '" + name + "' AND " + TABLE_CONDITION.SITUATION_ID + " = " + situationId, null, null, null, 
				null);
		c.moveToFirst();
		String note = c.getString(c.getColumnIndex(TABLE_CONDITION.NOTE));
		c.close();
		
		return note;
	}
	
	public boolean hasThisCondition(String title, long situationId){
		Cursor cur = db.query(TABLE_CONDITION.SQL_TABLE_NAME, 
				new String[]{
				TABLE_CONDITION.TITLE+" AS _id "}, TABLE_CONDITION.TITLE + " = '"+title+"' AND "+TABLE_CONDITION.SITUATION_ID + " = " + situationId, null, null, null, 
				null);			
		boolean answer = false;
		if(cur.getCount()>0){
			answer=true;
		}
		cur.close();
		return answer;
	}
	
	
	
	public void stop(){
		helper.close();
		db.close();
	}
}
