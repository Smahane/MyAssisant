package com.phonesettings.myassistant.conditions;

import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.myassistant.R;
import com.phonesettings.myassistant.db.ConditionManager;
import com.phonesettings.myassistant.db.DatabaseHelper;
import com.phonesettings.myassistant.db.TimeAlarmManager;
import com.phonesettings.myassistant.services.TimeReceiver;

public class ConditionsTimeActivity extends Activity {
	private static final int TIME_START_DIALOG = 0;
	private static final int TIME_END_DIALOG = 1;

	ConditionManager conditionsManager;
	TimeAlarmManager timeAlarmManager;
	private boolean isUpdate;
	private String title;
	private long sitId;

	private int mStartHour = 8;
	private int mStartMinute = 0;

	private int mEndHour = 17;
	private int mEndMinute = 0;

	private Button mStartButton;
	private Button mEndButton;
	private Button mRepeatButton;

	private boolean checkedDays[] = { true, true, true, true, true, true, true };

	final String daysOfWeek[] = { getFullDayName(0), getFullDayName(1),
			getFullDayName(2), getFullDayName(3), getFullDayName(4),
			getFullDayName(5), getFullDayName(6) };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.conditions_time);

		conditionsManager = new ConditionManager(ConditionsTimeActivity.this);
		timeAlarmManager = new TimeAlarmManager(ConditionsTimeActivity.this);

		Intent intent = getIntent();
		sitId = intent.getLongExtra("situationId", -1);
		title = getResources().getResourceEntryName(R.string.time);
		isUpdate = conditionsManager.hasThisCondition(title, sitId);

		mStartButton = (Button) findViewById(R.id.start_button);
		mEndButton = (Button) findViewById(R.id.end_button);
		mRepeatButton = (Button) findViewById(R.id.repeat_button);
		// get the current time
		// final Calendar c = Calendar.getInstance();
		// mStartHour = c.get(Calendar.HOUR_OF_DAY);
		// mStartMinute = c.get(Calendar.MINUTE);

		if (isUpdate) {
			String note = conditionsManager.getNote(title, sitId);
			String data[] = note.split("#");
			String startData[] = data[0].split(":");
			String endData[] = data[1].split(":");
			mStartHour = Integer.valueOf(startData[0]);
			mStartMinute = Integer.valueOf(startData[1]);
			mEndHour = Integer.valueOf(endData[0]);
			mEndMinute = Integer.valueOf(endData[1]);

			for (int i = 0; i < checkedDays.length; i++) {
				checkedDays[i] = false;
			}
			for (int i = 2; i < data.length; i++) {
				checkedDays[Integer.valueOf(data[i])] = true;
			}
			// cancel all alarms for this situation
			cancelAlarmsForSituation(sitId);
			// delete all alarms for this situation in DB
			timeAlarmManager.deleteAllAlarmsForSituation(sitId);
		}

		updateStartDisplay();
		updateEndDisplay();
		updateRepeatDisplay();
	}

	public void onStartClick(View v) {
		showDialog(TIME_START_DIALOG);
	}

	public void onEndClick(View v) {
		showDialog(TIME_END_DIALOG);
	}

	public void onRepeatClick(View v) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.repeat);
		builder.setMultiChoiceItems(daysOfWeek, checkedDays,
				new OnMultiChoiceClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which,
							boolean isChecked) {
						if (!isChecked) {
							checkedDays[which] = false;
						} else
							checkedDays[which] = true;
					}
				});
		builder.setPositiveButton(R.string.ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				updateRepeatDisplay();
			}
		});

		builder.setNeutralButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	// updates the start time we display in the Button
	private void updateStartDisplay() {
		mStartButton.setText(new StringBuilder().append(pad(mStartHour))
				.append(":").append(pad(mStartMinute)));
	}

	// updates the start time we display in the Button
	private void updateEndDisplay() {
		mEndButton.setText(new StringBuilder().append(pad(mEndHour))
				.append(":").append(pad(mEndMinute)));
	}

	private void updateRepeatDisplay() {
		String days = "";
		for (int i = 0; i < daysOfWeek.length; i++) {
			if (checkedDays[i]) {

				days += getShortDayName(i);
				days += " ";
			}
		}
		mRepeatButton.setText(days);
	}

	private static String pad(int c) {
		if (c >= 10) {
			return String.valueOf(c);
		} else {
			return "0" + String.valueOf(c);
		}
	}

	// the callback received when the user "sets" the start time in the dialog
	private TimePickerDialog.OnTimeSetListener mTimeSetStartListener = new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mStartHour = hourOfDay;
			mStartMinute = minute;
			updateStartDisplay();
		}
	};

	private TimePickerDialog.OnTimeSetListener mTimeSetEndListener = new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			mEndHour = hourOfDay;
			mEndMinute = minute;
			updateEndDisplay();
		}
	};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_START_DIALOG:
			return new TimePickerDialog(this, mTimeSetStartListener,
					mStartHour, mStartMinute, true);
		case TIME_END_DIALOG:
			return new TimePickerDialog(this, mTimeSetEndListener, mEndHour,
					mEndMinute, true);
		}
		return null;
	}

	public static String getFullDayName(int day) {
		Calendar c = Calendar.getInstance();
		// date doesn't matter - it has to be a Monday
		// I new that first August 2011 is one ;-)
		c.set(2011, 7, 1, 0, 0, 0);
		c.add(Calendar.DAY_OF_MONTH, day);
		return String.format("%tA", c);
	}

	public static String getShortDayName(int day) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2011, 7, 1, 0, 0, 0);
		calendar.add(Calendar.DAY_OF_MONTH, day);
		return String.format("%ta", calendar);
	}

	@Override
	public void onBackPressed() {
		if (mStartHour > mEndHour
				|| (mStartHour == mEndHour && mStartMinute > mEndMinute)) {
			Toast.makeText(ConditionsTimeActivity.this,
					getString(R.string.time_not_correct), Toast.LENGTH_SHORT)
					.show();
			return;
		}
		String note = mStartHour + ":" + mStartMinute + "#" + mEndHour + ":"
				+ mEndMinute;
		String repeatedDays = "";
		for (int i = 0; i < checkedDays.length; i++) {
			if (checkedDays[i] == true) {
				repeatedDays += "#" + i;
			}
		}
		note += repeatedDays;
		String desc = mStartButton.getText().toString();
		desc += " - " + mEndButton.getText().toString();
		desc += " " + mRepeatButton.getText().toString();

		if (!isUpdate) {
			conditionsManager.addCondition(title, sitId, desc, note);
		} else {
			conditionsManager.updateCondition(title, sitId, desc, note);
		}

		long alarmIds[] = insertAlarmsForSituation(sitId, mStartHour,
				mStartMinute, mEndHour, mEndMinute, checkedDays);
		ArrayList<Integer> days = getCheckedDays(checkedDays);
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

		// Set Start Alarms
		for (int it = 0; it < days.size(); it++) {
			// do not move timeOff initializing from here!
			Calendar timeOff = Calendar.getInstance();
			int currDay = timeOff.get(Calendar.DAY_OF_WEEK);
			int diff = ConditionsTimeActivity.getDaysDifference(currDay,
					days.get(it));
			timeOff.add(Calendar.DAY_OF_WEEK, diff);
			timeOff.set(Calendar.HOUR_OF_DAY, mStartHour);
			timeOff.set(Calendar.MINUTE, mStartMinute);
			timeOff.set(Calendar.SECOND, 0);

			Intent intent = new Intent(this, TimeReceiver.class);// do not
																	// change
																	// alarmIds
																	// counter
			intent.setAction(TimeReceiver.SET_ALARM);
			PendingIntent sender = PendingIntent.getBroadcast(
					getApplicationContext(), (int) alarmIds[it], intent,
					PendingIntent.FLAG_UPDATE_CURRENT);

			alarm.setRepeating(AlarmManager.RTC_WAKEUP,
					timeOff.getTimeInMillis(), 7 * AlarmManager.INTERVAL_DAY,
					sender);
		}
		// Set End Alarms
		for (int it = 0; it < days.size(); it++) {
			// do not move timeOff initializing from here!
			Calendar timeOff = Calendar.getInstance();
			int currDay = timeOff.get(Calendar.DAY_OF_WEEK);
			int diff = ConditionsTimeActivity.getDaysDifference(currDay,
					days.get(it));

			timeOff.add(Calendar.DAY_OF_WEEK, diff);
			timeOff.set(Calendar.HOUR_OF_DAY, mEndHour);
			timeOff.set(Calendar.MINUTE, mEndMinute);
			timeOff.set(Calendar.SECOND, 0);

			Intent intent = new Intent(this, TimeReceiver.class);// do not
																	// change
																	// alarmIds
																	// counter
			intent.setAction(TimeReceiver.SET_ALARM);
			PendingIntent sender = PendingIntent.getBroadcast(
					getApplicationContext(), (int) alarmIds[it + days.size()],
					intent, PendingIntent.FLAG_UPDATE_CURRENT);

			alarm.setRepeating(AlarmManager.RTC_WAKEUP,
					timeOff.getTimeInMillis(), 7 * AlarmManager.INTERVAL_DAY,
					sender);
		}

		super.onBackPressed();
	}

	// parser 12:00#14:00#0#1#5#6

	public long[] insertAlarmsForSituation(long sitId, int mStartHour,
			int mStartMinute, int mEndHour, int mEndMinute,
			boolean repeatedDays[]) {
		ArrayList<Integer> days = getCheckedDays(repeatedDays);
		long[] alarmIds = new long[days.size() * 2];
		int i = 0;
		for (; i < days.size(); i++) {
			alarmIds[i] = timeAlarmManager.addNewAlarm(sitId, mStartHour,
					mStartMinute, days.get(i));
		}

		for (int count = 0; i < days.size() * 2; i++, count++) {
			alarmIds[i] = timeAlarmManager.addNewAlarm(sitId, mEndHour,
					mEndMinute, days.get(count));
		}

		return alarmIds;
	}

	// Sunday = 1, Monday = 2 etc.
	public ArrayList<Integer> getCheckedDays(boolean repeatedDays[]) {
		ArrayList<Integer> days = new ArrayList<Integer>();

		// get the number of every day
		int j = 0;
		for (int i = 0; i < repeatedDays.length; i++) {
			if (repeatedDays[i]) {
				days.add(j, i + 2);
				j++;
			}
			// if sunday is repeating
			if (i == repeatedDays.length - 1 && repeatedDays[i - 1]) {
				days.remove(j - 1);
				days.add(j - 1, Calendar.SUNDAY);
			}
		}

		return days;
	}

	public void cancelAlarmsForSituation(long situationId) {
		AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent intent = new Intent(TimeReceiver.SET_ALARM);

		// cancel all alarms by its IDs
		Cursor c = timeAlarmManager.getAllAlarmsForSituation(situationId);
		if (c != null)
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				int idIndex = c
						.getColumnIndex(DatabaseHelper.TABLE_TIME_ALARM.ID);
				long alarmId = c.getLong(idIndex);

				PendingIntent sender = PendingIntent.getBroadcast(
						getBaseContext(), (int) alarmId, intent, 0);
				alarm.cancel(sender);
			}
		c.close();
	}

	public static int getDaysDifference(int currentDay, int alarmDay) {
		int days = 0;
		if (currentDay > alarmDay) {
			days = 7 - currentDay + alarmDay;
		} else {
			days = alarmDay - currentDay;
		}
		return days;
	}

	@Override
	protected void onDestroy() {
		timeAlarmManager.stop();
		conditionsManager.stop();
		super.onDestroy();
	}

}
