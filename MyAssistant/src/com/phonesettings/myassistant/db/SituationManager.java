package com.phonesettings.myassistant.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.myassistant.R;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_CONDITION;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_SETTING;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_SITUATION;

public class SituationManager {
    private final DatabaseHelper helper;
    private final SQLiteDatabase db;
    private static int maxPosition = 0;

    public SituationManager(Context c) {
        helper = new DatabaseHelper(c);

        db = helper.getWritableDatabase();
        Cursor cursor = db.query(TABLE_SITUATION.SQL_TABLE_NAME, null, null,
                null, null, null, null);
        maxPosition = cursor.getCount();

        if (maxPosition == 0) {
            addSituation(c.getString(R.string.defaults));
        }

        cursor.close();
    }

    public int getMaxPosition() {
        return maxPosition;
    }

    public void setMaxPosition(int pos) {
        maxPosition = pos;
    }

    public long addSituation(String situationName) {

        maxPosition++;

        ContentValues values = new ContentValues();
        values.put(TABLE_SITUATION.IS_ACTIVE, true);
        values.put(TABLE_SITUATION.SITUATION_NAME, situationName);
        values.put(TABLE_SITUATION.POSITION, maxPosition);
        values.put(TABLE_SITUATION.RUN_STATUS, false);

        // Inserting Row
        long rowID = db.insert(TABLE_SITUATION.SQL_TABLE_NAME, null, values);
        // Closing database connection

        return rowID;
    }

    public boolean isThereSituationWithTheSameName(String name) {
        Cursor cursor = db.query(TABLE_SITUATION.SQL_TABLE_NAME,
                new String[] { TABLE_SITUATION.POSITION + " AS _id" },
                TABLE_SITUATION.SITUATION_NAME + "='" + name + "' ", null,
                null, null, null);
        int numRows = cursor.getCount();
        cursor.close();

        return numRows != 0;
    }

    public Cursor getAllSituations(boolean onlyActive) {
        String isActive = null;
        if (onlyActive) {
            isActive = TABLE_SITUATION.IS_ACTIVE + "=" + "1";
        }
        return db.query(TABLE_SITUATION.SQL_TABLE_NAME, new String[] {
                TABLE_SITUATION.ID, TABLE_SITUATION.SITUATION_NAME,
                TABLE_SITUATION.IS_ACTIVE, TABLE_SITUATION.POSITION,
                TABLE_SITUATION.RUN_STATUS},
                isActive, null, null, null, TABLE_SITUATION.POSITION + " DESC");
    }

    public void updateSituationActivity(boolean activity, String situationName) {
        ContentValues values = new ContentValues();
        values.put(TABLE_SITUATION.IS_ACTIVE, activity);
        if(!activity){
            values.put(TABLE_SITUATION.RUN_STATUS, activity);
        }
        db.update(TABLE_SITUATION.SQL_TABLE_NAME, values,
                TABLE_SITUATION.SITUATION_NAME + " = '" + situationName + "' ",
                null);
    }

    public void updateSituationRunStatus(boolean status, long situationId) {
        ContentValues values = new ContentValues();
        values.put(TABLE_SITUATION.RUN_STATUS, status);

        db.update(TABLE_SITUATION.SQL_TABLE_NAME, values,
                TABLE_SITUATION.ID + " = " + situationId, null);
    }

    public void updateSituationName(long sitId, String newName) {
        ContentValues values = new ContentValues();
        values.put(TABLE_SITUATION.SITUATION_NAME, newName);

        db.update(TABLE_SITUATION.SQL_TABLE_NAME, values, TABLE_SITUATION.ID
                + " = " + sitId, null);
    }

    public long getSituationIdByName(String name) {
        Cursor cursor = db.query(TABLE_SITUATION.SQL_TABLE_NAME,
                new String[] { TABLE_SITUATION.ID },
                TABLE_SITUATION.SITUATION_NAME + "='" + name + "' ", null,
                null, null, null);
        cursor.moveToFirst();
        long id = cursor.getLong(cursor.getColumnIndex(TABLE_SITUATION.ID));
        cursor.close();
        return id;
    }

    public int getSituationPosition(long sitId) {
        Cursor cursor = db.query(TABLE_SITUATION.SQL_TABLE_NAME,
                new String[] { TABLE_SITUATION.POSITION }, TABLE_SITUATION.ID
                + "=" + sitId, null, null, null, null);
        cursor.moveToFirst();
        int pos = cursor
                .getInt(cursor.getColumnIndex(TABLE_SITUATION.POSITION));
        cursor.close();
        return pos;
    }

    public void swapSituationPositions(long firstSituationId,
            long secondSituationId) {
        int firstPos = getSituationPosition(firstSituationId);
        int secondPos = getSituationPosition(secondSituationId);

        ContentValues value1 = new ContentValues();
        value1.put(TABLE_SITUATION.POSITION, secondPos);

        ContentValues value2 = new ContentValues();
        value2.put(TABLE_SITUATION.POSITION, firstPos);

        db.update(TABLE_SITUATION.SQL_TABLE_NAME, value1, TABLE_SITUATION.ID
                + "=" + firstSituationId, null);
        db.update(TABLE_SITUATION.SQL_TABLE_NAME, value2, TABLE_SITUATION.ID
                + "=" + secondSituationId, null);
    }

    public void deleteSituation(String name) {
        int sitPosition = 0;
        Cursor cursor = db.query(TABLE_SITUATION.SQL_TABLE_NAME,
                new String[] { TABLE_SITUATION.ID, TABLE_SITUATION.POSITION },
                TABLE_SITUATION.SITUATION_NAME + "='" + name + "' ", null,
                null, null, null);
        if (cursor.moveToFirst()) {
            sitPosition = cursor.getInt(cursor.getColumnIndex(TABLE_SITUATION.POSITION));
            long sitId = cursor.getInt(cursor
                    .getColumnIndex(TABLE_SITUATION.ID));
            db.delete(TABLE_CONDITION.SQL_TABLE_NAME,
                    TABLE_CONDITION.SITUATION_ID + "=" + sitId, null);
            db.delete(TABLE_SETTING.SQL_TABLE_NAME, TABLE_SETTING.SITUATION_ID
                    + "=" + sitId, null);
            db.delete(TABLE_SITUATION.SQL_TABLE_NAME,
                    TABLE_SITUATION.SITUATION_NAME + " = '" + name + "' ", null);
        }
        cursor.close();

        // Change position on the next situations with -1
        if(sitPosition != 0){
            Cursor c = db.query(TABLE_SITUATION.SQL_TABLE_NAME,
                    new String[] { TABLE_SITUATION.ID, TABLE_SITUATION.POSITION },
                    TABLE_SITUATION.POSITION + " > " + sitPosition, null,
                    null, null, TABLE_SITUATION.POSITION + " ASC");
            changePosition(c, -1);
            c.close();

            maxPosition -= 1;
        }
    }

    private void changePosition(Cursor cursor, int changeValue){
        if(cursor != null){
            for(cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()){
                int sitPosition = cursor.getInt(cursor.getColumnIndex(TABLE_SITUATION.POSITION));
                long sitId = cursor.getLong(cursor.getColumnIndex(TABLE_SITUATION.ID));
                ContentValues value = new ContentValues();
                value.put(TABLE_SITUATION.POSITION, sitPosition + changeValue);

                db.update(TABLE_SITUATION.SQL_TABLE_NAME, value, TABLE_SITUATION.ID
                        + "=" + sitId, null);
            }
        }
    }

    public boolean dragAndDropPositionsChange(int startPos, int endPos){
        if(startPos < 0 || endPos < 0){
            return false;
        }
        Cursor cursor = getSituationsBetweenPositions(startPos, endPos);
        if(startPos < endPos){
            changePosition(cursor, -1);
            cursor.moveToLast();

        }else if(startPos > endPos){
            changePosition(cursor, 1);
            cursor.moveToFirst();
        }

        long sitId = cursor.getLong(cursor.getColumnIndex(TABLE_SITUATION.ID));
        ContentValues value = new ContentValues();
        value.put(TABLE_SITUATION.POSITION, endPos);
        db.update(TABLE_SITUATION.SQL_TABLE_NAME, value, TABLE_SITUATION.ID
                + "=" + sitId, null);

        cursor.close();
        return true;
    }

    private Cursor getSituationsBetweenPositions(int startPos, int endPos){
        String where="";
        if(startPos < endPos){
            where = TABLE_SITUATION.POSITION + " >= " + startPos +
                    " AND " + TABLE_SITUATION.POSITION + " <= " + endPos;
        }
        else {
            where = TABLE_SITUATION.POSITION + " <= " + startPos +
                    " AND " + TABLE_SITUATION.POSITION + " >= " + endPos;
        }

        return db.query(TABLE_SITUATION.SQL_TABLE_NAME, new String[] {
                TABLE_SITUATION.ID, TABLE_SITUATION.SITUATION_NAME,
                TABLE_SITUATION.IS_ACTIVE, TABLE_SITUATION.POSITION, TABLE_SITUATION.RUN_STATUS },
                where, null, null, null, TABLE_SITUATION.POSITION + " DESC");
    }

    public void stop() {
        helper.close();
        db.close();
    }
}
