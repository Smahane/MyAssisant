package com.phonesettings.myassistant;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.example.myassistant.R;
import com.phonesettings.myassistant.adapter.EditSituationAdapter;
import com.phonesettings.myassistant.conditions.ConditionsContactActivity;
import com.phonesettings.myassistant.conditions.ConditionsLocationActivity;
import com.phonesettings.myassistant.conditions.ConditionsTimeActivity;
import com.phonesettings.myassistant.db.ConditionManager;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_TIME_ALARM;
import com.phonesettings.myassistant.db.MarkerManager;
import com.phonesettings.myassistant.db.SettingManager;
import com.phonesettings.myassistant.db.SituationManager;
import com.phonesettings.myassistant.db.TimeAlarmManager;
import com.phonesettings.myassistant.services.TimeReceiver;
import com.phonesettings.myassistant.settings.SettingsVolumeActivity;
import com.phonesettings.myassistant.utils.C;

public class EditSituationActivity extends Activity {

	private ConditionManager conditionManager = null;
	private SettingManager settingManager = null;
	private Cursor cursorConditions = null;
	private Cursor cursorSettings = null;
	private SimpleCursorAdapter conditionsAdapter = null;
	private SimpleCursorAdapter settingsAdapter = null;
	private ListView conditionsList;
	private ListView settingsList;
	private EditText sitName;

	private long situationId = -1;

	SharedPreferences pre_pref;

	SharedPreferences.Editor pre_editor;

	String cr, cn;
	int v_mode, r_v, n_v;
	AudioManager am;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_situation);

		pre_pref = getSharedPreferences("presets", MODE_PRIVATE);
		pre_editor = pre_pref.edit();

		pre_pref = getSharedPreferences("presets", MODE_WORLD_READABLE);

		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		v_mode = am.getRingerMode();

		conditionManager = new ConditionManager(EditSituationActivity.this);
		settingManager = new SettingManager(EditSituationActivity.this);

		conditionsList = (ListView) findViewById(R.id.conditions_list);
		settingsList = (ListView) findViewById(R.id.settings_list);
		sitName = (EditText) findViewById(R.id.sit_name_et);

		Intent intent = getIntent();
		if (intent.hasExtra("situationId")) {
			situationId = intent.getLongExtra("situationId", -1);
			String name = intent.getStringExtra("sitName");
			sitName.setText(name);

			if (name != null && name.equals(getString(R.string.defaults))) {
				TextView condTitle = (TextView) findViewById(R.id.conditions_title_txt);
				LinearLayout defaultConditions = (LinearLayout) findViewById(R.id.conditions_part);
				condTitle.setVisibility(View.GONE);
				defaultConditions.setVisibility(View.GONE);
				sitName.setEnabled(false);
			}
		}
	}

	// final int settings[] = { R.string.ringtone, R.string.volume };
	//
	// final int conditions[] = { R.string.contact, R.string.location,
	// R.string.time };

	final int settings[] = { R.string.ringtone, R.string.volume };

	final int conditions[] = { R.string.contact, R.string.location,
			R.string.time };

	public void AddNewCondition(View v) {
		chooseOption(conditions, R.string.add_condition);
	}

	public void AddNewSetting(View v) {
		chooseOption(settings, R.string.add_setting);
	}

	// Click on item of ListView
	public void onEditList(View v) {
		TextView text = (TextView) v.findViewById(R.id.edit_title);
		String title = text.getHint().toString();
		int resId = getResources().getIdentifier(title, "string",
				getPackageName());

		selectItem(resId);
	}

	public void onDelete(View v) {
		View layout = (View) v.getParent();
		TextView text = (TextView) layout.findViewById(R.id.edit_title);
		String title = text.getHint().toString();

		conditionManager.deleteCondition(title, situationId);
		updateConditionsList();

		settingManager.deleteSetting(title, situationId);
		updateSettingsList();

		if (text.getText() != null
				&& text.getText().equals(getString(R.string.time))) {
			AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
			Intent intent = new Intent(TimeReceiver.SET_ALARM);

			// cancel all alarms by its IDs
			TimeAlarmManager timeAlarmMng = new TimeAlarmManager(
					EditSituationActivity.this);
			Cursor c = timeAlarmMng.getAllAlarmsForSituation(situationId);
			if (c != null)
				for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
					int idIndex = c.getColumnIndex(TABLE_TIME_ALARM.ID);
					long alarmId = c.getLong(idIndex);

					PendingIntent sender = PendingIntent.getBroadcast(
							getBaseContext(), (int) alarmId, intent, 0);
					alarm.cancel(sender);
				}
			c.close();
			// delete all alarms for this situation in DB
			timeAlarmMng.deleteAllAlarmsForSituation(situationId);
			timeAlarmMng.stop();
		} else if (text.getText() != null
				&& text.getText().equals(getString(R.string.location))) {
			MarkerManager mm = new MarkerManager(this);
			mm.deleteAllLocationsForSituation(situationId);
			mm.stop();
		}
	}

	private void updateConditionsList() {
		cursorConditions = conditionManager
				.getAllConditionsForSituation(situationId);
		startManagingCursor(cursorConditions);
		conditionsAdapter = new EditSituationAdapter(
				EditSituationActivity.this, R.layout.row_edit,
				cursorConditions, new String[] {},// name of column from DB
				new int[] {}, true);// id of view

		conditionsList.setAdapter(conditionsAdapter);

		TextView noCondition = (TextView) findViewById(R.id.no_condition);
		if (cursorConditions.getCount() > 0) {
			noCondition.setVisibility(View.GONE);
		} else {
			noCondition.setVisibility(View.VISIBLE);
		}

		setListViewHeightBasedOnChildren(conditionsList);
	}

	private void updateSettingsList() {
		cursorSettings = settingManager.getAllSettingsForSituation(situationId);
		startManagingCursor(cursorSettings);
		settingsAdapter = new EditSituationAdapter(EditSituationActivity.this,
				R.layout.row_edit, cursorSettings, new String[] {},// name of
				// column
				// from DB
				new int[] {}, false);// id of view

		settingsList.setAdapter(settingsAdapter);

		TextView noSetting = (TextView) findViewById(R.id.no_setting);
		if (cursorSettings.getCount() > 0) {
			noSetting.setVisibility(View.GONE);
		} else {
			noSetting.setVisibility(View.VISIBLE);
		}
		setListViewHeightBasedOnChildren(settingsList);
	}

	// Dialog for settings and conditions
	private void chooseOption(final int array[], int title) {

		final ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();

		HashMap<String, Object> map;
		for (int i = 0; i < array.length; i++) {
			if (!conditionManager.hasThisCondition(getResources()
					.getResourceEntryName(array[i]), situationId)
					& !settingManager.hasThisSetting(getResources()
							.getResourceEntryName(array[i]), situationId)) {
				map = new HashMap<String, Object>();
				map.put("iconID", C.getInstance().getIconIdByTitle(array[i])
						+ "");
				map.put("text", getString(array[i]));
				map.put("string_id", array[i]);
				mylist.add(map);
			}
		}

		ListAdapter mSchedule = new SimpleAdapter(this, mylist,
				R.layout.row_dialog, new String[] { "iconID", "text" },
				new int[] { R.id.dialog_icon, R.id.dialog_text });

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(title);
		builder.setAdapter(mSchedule, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				int objId = (Integer) mylist.get(item).get("string_id");
				selectItem(objId);
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	@Override
	protected void onResume() {
		updateConditionsList();
		updateSettingsList();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		conditionManager.stop();
		settingManager.stop();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		String name = sitName.getText().toString();
		TextView noSetting = (TextView) findViewById(R.id.no_setting);
		TextView noCondition = (TextView) findViewById(R.id.no_condition);
		if (name.equals("") && noSetting.isShown() && noCondition.isShown()) {
			SituationManager situationManager = new SituationManager(this);
			situationManager.deleteSituation(name);
			situationManager.stop();
			setResult(Activity.RESULT_CANCELED);
			super.onBackPressed();
		} else if (name.equals("")) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.no_situation_name)
					.setMessage(R.string.add_situation_name)
					.setCancelable(false)
					.setPositiveButton(R.string.ok,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.show();
		} else {
			SituationManager situationManager = new SituationManager(this);
			situationManager.updateSituationName(situationId, name);
			situationManager.stop();

			Intent service = new Intent(EditSituationActivity.this,
					SettingsMaker.class);
			startService(service);
			// SettingsMaker.getInstance(getApplicationContext()).update();

			setResult(Activity.RESULT_OK);
			super.onBackPressed();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_help:
			Intent i = new Intent(this, HelpActivity.class);
			startActivity(i);
			break;
		// case R.id.menu_dont_save:
		// TODO:
		// setResult(Activity.RESULT_CANCELED);
		// finish();
		// break;
		case R.id.menu_save:
			getringvol();
			getnotvol();
			onBackPressed();
			break;
		}

		return false;
	}

	private void selectItem(int choice) {
		Intent intent = null;

		switch (choice) {

		case R.string.contact:
			intent = new Intent(EditSituationActivity.this,
					ConditionsContactActivity.class);
			break;
		case R.string.location:
			intent = new Intent(EditSituationActivity.this,
					ConditionsLocationActivity.class);
			intent.putExtra("situationId", situationId);
			startActivityForResult(intent, LOCATION_REQUEST_CODE);
			return;
			// break;

		case R.string.time:
			intent = new Intent(EditSituationActivity.this,
					ConditionsTimeActivity.class);

			break;

		case R.string.ringtone:
			intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,
					RingtoneManager.TYPE_RINGTONE);
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
					RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI);
			// intent.putExtra( RingtoneManager.EXTRA_RINGTONE_TITLE,
			// "Select Tone");

			startActivityForResult(intent, C.RINGTONE_REQUEST);
			return;

		case R.string.volume:
			intent = new Intent(EditSituationActivity.this,
					SettingsVolumeActivity.class);
			break;
		// case R.string.wallpaper:
		// intent = new Intent(EditSituationActivity.this,
		// SettingsWallpaperActivity.class);
		// break;
		// case R.string.wi_fi:
		// intent = new Intent(EditSituationActivity.this,
		// SettingsWiFiActivity.class);
		// break;
		}

		intent.putExtra("situationId", situationId);
		startActivity(intent);
	}

	final int LOCATION_REQUEST_CODE = 102;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == C.RINGTONE_REQUEST) {

				String title = getResources().getResourceEntryName(
						R.string.ringtone);
				boolean isUpdate = settingManager.hasThisSetting(title,
						situationId);

				String note = "";
				Uri uri = data
						.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				if (uri != null) {
					note = uri.toString();
				}

				Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
				String desc = getString(R.string.unknown);
				if (ringtone != null) {
					desc = ringtone.getTitle(this);
				} else {
					desc = getString(R.string.silent);
				}

				if (isUpdate) {
					settingManager
							.updateSetting(title, situationId, desc, note);
				} else {
					settingManager.addSetting(title, situationId, desc, note);
				}
			}
		} else if (resultCode == Activity.RESULT_CANCELED
				&& requestCode == LOCATION_REQUEST_CODE) {
			MarkerManager mm = new MarkerManager(this);
			mm.deleteAllLocationsForSituation(situationId);
			mm.stop();

			String title = getResources().getResourceEntryName(
					R.string.location);
			conditionManager.deleteCondition(title, situationId);
			updateConditionsList();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	// ListView in ScrollView fix
	public static void setListViewHeightBasedOnChildren(ListView listView) {
		ListAdapter listAdapter = listView.getAdapter();
		if (listAdapter == null) {
			return;
		}

		int totalHeight = 0;
		int desiredWidth = MeasureSpec.makeMeasureSpec(listView.getWidth(),
				MeasureSpec.UNSPECIFIED);
		for (int i = 0; i < listAdapter.getCount(); i++) {
			View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(desiredWidth, MeasureSpec.UNSPECIFIED);
			totalHeight += listItem.getMeasuredHeight();
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight
				+ (listView.getDividerHeight() * (listAdapter.getCount() - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
	}

	public void getringvol() {
		Uri uri1 = RingtoneManager.getActualDefaultRingtoneUri(
				getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
		if (uri1 != null) {
			cr = uri1.toString();
		} else {
			cr = null;
		}
		Log.e("cr", cr);
		if (v_mode == 0) {
			r_v = 0;
		} else if (v_mode == 1) {
			r_v = 0;
		} else if (v_mode == 2) {
			r_v = 5;
		}

		pre_editor.putString("cr_uri", cr);
		pre_editor.putInt("r_v", r_v);
		pre_editor.putInt("v_mode", v_mode);
		pre_editor.commit();
	}

	public void getnotvol() {
		Uri uri2 = RingtoneManager.getActualDefaultRingtoneUri(
				getApplicationContext(), RingtoneManager.TYPE_NOTIFICATION);
		if (uri2 != null) {
			cn = uri2.toString();
		} else {
			cn = null;
		}
		Log.e("cn", cn);
		if (v_mode == 0) {
			n_v = 0;
		} else if (v_mode == 1) {
			n_v = 0;
		} else if (v_mode == 2) {
			n_v = 5;
		}

		pre_editor.putString("nr_uri", cn);
		pre_editor.putInt("n_v", n_v);
		pre_editor.commit();
	}
}