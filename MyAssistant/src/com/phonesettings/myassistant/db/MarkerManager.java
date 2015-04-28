package com.phonesettings.myassistant.db;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_LOCATION;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_SITUATION;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class MarkerManager {
    private DatabaseHelper helper;
    private SQLiteDatabase db;
    public MarkerManager(Context c){
        helper = new DatabaseHelper(c);
        db = helper.getWritableDatabase();
    }

    public long addLocation(long situationId, double lat, double lon, int radius, String address) {
        ContentValues values = new ContentValues();
        values.put(TABLE_LOCATION.SITUATION_ID, situationId);
        values.put(TABLE_LOCATION.LATITUDE, String.valueOf(lat));
        values.put(TABLE_LOCATION.LONGITUDE, String.valueOf(lon));
        values.put(TABLE_LOCATION.RADIUS, radius);
        values.put(TABLE_LOCATION.ADDRESS, address);
        values.put(TABLE_LOCATION.IS_FAVORITE, "0");

        // Inserting Row
        long rowID = db.insert(TABLE_LOCATION.SQL_TABLE_NAME, null, values);

        return rowID;
    }

    public void updateMarker(long id, long situationId, double lat,
            double lon, int radius, String address) {
        ContentValues values = new ContentValues();
        values.put(TABLE_LOCATION.LATITUDE, String.valueOf(lat));
        values.put(TABLE_LOCATION.LONGITUDE, String.valueOf(lon));
        values.put(TABLE_LOCATION.RADIUS, radius);
        values.put(TABLE_LOCATION.ADDRESS, address);

        db.update(TABLE_LOCATION.SQL_TABLE_NAME, values, TABLE_LOCATION.ID
                + " = " + id + " AND " + TABLE_LOCATION.SITUATION_ID
                + " = " + situationId, null);
    }

    public void updateRadius(long id, int radius){
        ContentValues values = new ContentValues();
        values.put(TABLE_LOCATION.RADIUS, radius);

        db.update(TABLE_LOCATION.SQL_TABLE_NAME, values, TABLE_LOCATION.ID
                + " = " + id, null);
    }

    public Cursor getAllLocations(boolean onlyForEnabledSituations){
        String where = null;
        if(onlyForEnabledSituations){
            where = TABLE_SITUATION.SQL_TABLE_NAME + "."+TABLE_SITUATION.IS_ACTIVE + " = " + "1";
        }

        return db.query(TABLE_LOCATION.SQL_TABLE_NAME,
                new String[]{
                TABLE_LOCATION.ID + " AS _id",
                TABLE_LOCATION.SITUATION_ID,
                TABLE_LOCATION.LATITUDE,
                TABLE_LOCATION.LONGITUDE,
                TABLE_LOCATION.RADIUS,
                TABLE_LOCATION.ADDRESS},
                where, null, null, null, null);
    }

    public Cursor getAllLocationsForSituation(long situationId){
        return  db.query(TABLE_LOCATION.SQL_TABLE_NAME,
                new String[]{
                TABLE_LOCATION.ID /*+ " AS _id"*/,
                TABLE_LOCATION.SITUATION_ID,
                TABLE_LOCATION.LATITUDE,
                TABLE_LOCATION.LONGITUDE,
                TABLE_LOCATION.RADIUS,
                TABLE_LOCATION.ADDRESS},
                TABLE_LOCATION.SITUATION_ID + " = " + situationId, null, null, null, null);
    }

    public int deleteLocation(long situationId, long locId){
        return db.delete(TABLE_LOCATION.SQL_TABLE_NAME, TABLE_LOCATION.SITUATION_ID+
                " = "+situationId+" AND "+ TABLE_LOCATION.ID+"="+locId, null);
    }

    public int deleteAllLocationsForSituation(long situationId){
        return db.delete(TABLE_LOCATION.SQL_TABLE_NAME, TABLE_LOCATION.SITUATION_ID+
                " = "+situationId, null);
    }

    public void stop(){
        helper.close();
        db.close();
    }
}
