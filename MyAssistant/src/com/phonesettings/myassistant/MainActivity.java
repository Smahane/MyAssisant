package com.phonesettings.myassistant;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.myassistant.R;
import com.phonesettings.myassistant.adapter.MainAdapter;
import com.phonesettings.myassistant.db.DatabaseHelper;
import com.phonesettings.myassistant.db.MarkerManager;
import com.phonesettings.myassistant.db.SituationManager;
import com.phonesettings.myassistant.services.LocationService;
import com.phonesettings.myassistant.utils.C;

public class MainActivity extends ListActivity {
	private static String TAG = "MainActivity";

	private SituationManager situationManager;

	private BroadcastReceiver mReceiver;

	ToggleButton exchangeButton;

	Cursor cursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		C.getInstance();
		if (!LocationService.isRunning()) {
			startService(new Intent(this, LocationService.class));
		}
		situationManager = new SituationManager(MainActivity.this);
		exchangeButton = (ToggleButton) findViewById(R.id.exchange_button);

		SharedPreferences sp = getSharedPreferences(C.prefs.PREFS_NAME,
				MODE_WORLD_READABLE);
		Editor editor = sp.edit();
		editor.putBoolean(C.prefs.not_exit_from_app, true);
		editor.commit();
	}

	@Override
	protected void onResume() {
		updateList();
		ListView listView = getListView();

		if (listView instanceof DragAndDropListView) {
			((DragAndDropListView) listView).setDropListener(mDropListener);
			((DragAndDropListView) listView).setDragListener(mDragListener);
		}

		startService(new Intent(MainActivity.this, SettingsMaker.class));
		IntentFilter intentFilter = new IntentFilter(
				"android.intent.action.MAIN");
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				cursor.requery();
			}
		};
		registerReceiver(mReceiver, intentFilter);

		super.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	@Override
	protected void onDestroy() {
		situationManager.stop();
		super.onDestroy();
	}

	private DragAndDropListView.DropListener mDropListener = new DragAndDropListView.DropListener() {

		@Override
		public void onDrop(int from, int to) {
			ListAdapter adapter = getListAdapter();
			exchangeButton.setChecked(false);
			if (from == to)
				return;
			if (adapter instanceof MainAdapter) {
				startService(new Intent(MainActivity.this, SettingsMaker.class));
			}
		}
	};

	private DragAndDropListView.DragListener mDragListener = new DragAndDropListView.DragListener() {

		@Override
		public void onChangePosition(int from, int to) {
			situationManager.dragAndDropPositionsChange(cursor.getCount()
					- from, cursor.getCount() - to);
			cursor.requery();
		}
	};

	public void onToggleClick(View v) {
		TextView tvName = (TextView) ((RelativeLayout) v.getParent())
				.findViewById(R.id.situations_name);
		String situationName = tvName.getText().toString();

		situationManager.updateSituationActivity(
				((ToggleButton) v).isChecked(), situationName);
		startService(new Intent(MainActivity.this, SettingsMaker.class));// update
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		TextView name = (TextView) v.findViewById(R.id.situations_name);
		long situationId = situationManager.getSituationIdByName(name.getText()
				.toString());

		Intent i = new Intent(MainActivity.this, EditSituationActivity.class);
		i.putExtra("situationId", situationId);
		i.putExtra("sitName", name.getText().toString());
		startActivityForResult(i, C.EDIT_SITUATION);

	}
//
//	public void onSituationsExchange(View v) {
//		((DragAndDropListView) getListView()).enableSorting(exchangeButton
//				.isChecked());
//	}

	public void onDeleteSituation(View v) {
		final int position = getListView().getPositionForView(
				(RelativeLayout) v.getParent());
		if (position < 0)
			return;
		Cursor c = (Cursor) getListAdapter().getItem(position);
		int mColumnNameIndex = c
				.getColumnIndex(DatabaseHelper.TABLE_SITUATION.SITUATION_NAME);
		final String name = c.getString(mColumnNameIndex);
		String message = getString(R.string.sure_asking);
		message += " \"" + name + "\" ?";
		TextView hint = new TextView(v.getContext());
		hint.setText(getString(R.string.no_undone));
		hint.setPadding(30, 0, 0, 10);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.confirm_remove)
				.setMessage(message)
				.setView(hint)
				.setCancelable(false)
				.setPositiveButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						})
				.setNegativeButton(R.string.remove,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								long sitId = situationManager
										.getSituationIdByName(name);
								// Delete all location for this situation if
								// exist
								MarkerManager mm = new MarkerManager(
										MainActivity.this);
								mm.deleteAllLocationsForSituation(sitId);
								mm.stop();

								situationManager.deleteSituation(name);
								cursor.requery();

								startService(new Intent(MainActivity.this,
										SettingsMaker.class));
							}
						});

		AlertDialog alert = builder.create();
		alert.show();

		TextView messageView = (TextView) alert
				.findViewById(android.R.id.message);
		messageView.setGravity(Gravity.LEFT);
	}

	public void AddNewSituation(View v) {
		long value = situationManager.addSituation("");
		Intent i = new Intent(MainActivity.this, EditSituationActivity.class);
		i.putExtra("situationId", value);
		startActivityForResult(i, C.ADD_SITUATION);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == C.EDIT_SITUATION) {
			if (resultCode == Activity.RESULT_OK) {
			} else if (resultCode == Activity.RESULT_CANCELED) {
			}
		}
		if (requestCode == C.ADD_SITUATION) {
			if (resultCode == Activity.RESULT_OK) {
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private void updateList() {
		cursor = situationManager.getAllSituations(false);
		startManagingCursor(cursor);
		SimpleCursorAdapter adapter = new MainAdapter(MainActivity.this,
				R.layout.row_main_situations, cursor, new String[] {},// name
				// of
				// column
				// from
				// DB
				new int[] {});// id of view
		setListAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i = null;

		switch (item.getItemId()) {
		case R.id.menu_add_situation:
			Button addSit = (Button) findViewById(R.id.footer_add_situation);
			AddNewSituation(addSit);
			break;

		case R.id.menu_help:
			i = new Intent(this, HelpActivity.class);
			startActivity(i);
			break;

		case R.id.menu_exit:
			wantExitDialog();
			break;
		}
		return false;
	}

	private void wantExitDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.want_exit_msg)
				.setCancelable(false)
				.setPositiveButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								stopService(new Intent(MainActivity.this,
										LocationService.class));
								dialog.cancel();
							}
						})
				.setNegativeButton(R.string.exit,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								SharedPreferences sp = getSharedPreferences(
										C.prefs.PREFS_NAME, MODE_WORLD_READABLE);
								Editor editor = sp.edit();
								editor.putBoolean(C.prefs.not_exit_from_app,
										false);
								editor.commit();
								NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
								nm.cancelAll();
								finish();
							}
						});
		AlertDialog alert = builder.create();
		alert.show();
	}
}