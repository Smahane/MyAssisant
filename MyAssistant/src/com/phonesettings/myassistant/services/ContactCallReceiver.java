package com.phonesettings.myassistant.services;

import java.util.Random;

import com.phonesettings.myassistant.SettingsMaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ContactCallReceiver extends BroadcastReceiver {

	private static final String TAG = "ContactCallReceiver";
	private Context _context;
	@Override
	public void onReceive(Context context, Intent intent) {
		_context = context;
		TelephonyManager telephony = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		CustomPhoneStateListener customPhoneListener = new CustomPhoneStateListener();

		telephony.listen(customPhoneListener,
				PhoneStateListener.LISTEN_CALL_STATE);

		Bundle bundle = intent.getExtras();
		String phoneNr="";
		if(intent.hasExtra(TelephonyManager.EXTRA_INCOMING_NUMBER))
			phoneNr = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
		Log.v(TAG, "ContactCallReceiver::onReceive!");
		Log.v(TAG, "phoneNr: " + phoneNr);
	}

	private class CustomPhoneStateListener extends PhoneStateListener {

		private static final String TAG = "CustomPhoneStateListener";

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {

			Log.v(TAG, "ContactCallReceiver::onCallStateChanged!");
			Log.v(TAG, incomingNumber);

			Intent service = new Intent(_context.getApplicationContext(), SettingsMaker.class);
			switch (state) {
			case TelephonyManager.CALL_STATE_RINGING:
				Log.d(TAG, "RINGING");
				SettingsMaker.contactValue = incomingNumber;
				//SettingsMaker.getInstance(_context).update();
				
				_context.startService(service);
				break;
			case TelephonyManager.CALL_STATE_IDLE:	
				SettingsMaker.contactValue = "a";				
				//SettingsMaker.getInstance(_context).update();
				_context.startService(service);
				break;
			}
		}
	}
	
	public static int getContactIDFromNumber(String contactNumber,Context context)
	{
	    int phoneContactID = new Random().nextInt();
	    Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contactNumber));
	    Cursor contactLookupCursor = context.getContentResolver().query(uri, new String[] {PhoneLookup.DISPLAY_NAME, PhoneLookup._ID}, null, null, null);
	    if(contactLookupCursor.moveToFirst())
	    phoneContactID = contactLookupCursor.getInt(contactLookupCursor.getColumnIndexOrThrow(PhoneLookup._ID));	          
	    contactLookupCursor.close();

	    return phoneContactID;
	}

}
