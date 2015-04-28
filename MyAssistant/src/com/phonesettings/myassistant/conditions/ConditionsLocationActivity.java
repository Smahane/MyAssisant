package com.phonesettings.myassistant.conditions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.example.myassistant.R;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.phonesettings.myassistant.adapter.AutoCompleteAdapter;
import com.phonesettings.myassistant.db.ConditionManager;
import com.phonesettings.myassistant.db.DatabaseHelper.TABLE_LOCATION;
import com.phonesettings.myassistant.db.MarkerManager;
import com.phonesettings.myassistant.services.LocationReceiver;
import com.phonesettings.myassistant.services.OnNewLocationListener;
import com.phonesettings.myassistant.utils.C;
import com.phonesettings.myassistant.utils.Utils;

public class ConditionsLocationActivity extends FragmentActivity {
	private final String TAG = "ConditionsLocationActivity";

	GoogleMap mapView;
	Geocoder geocoder;
	ProgressBar pbLoading;
	AutoCompleteTextView search;
	TextView tvRadius;
	SeekBar radiusSeekbar;
	Location location;

	ImageButton ibSearch;
	ImageButton ibRadius;
	ImageButton ibAddLocation;
	ImageButton ibRemoveLocation;

	private String desc;
	private boolean isUpdate;
	private long sitId;
	private String title;
	private ConditionManager conditionsManager;

	final String keyMarker = "marker";
	final String keyCircle = "circle";
	final String keyId = "id";
	private HashMap<LatLng, HashMap<String, Object>> locDrawables;
	private Circle currentCircle;
	private Marker currentMarker;
	private double currentRadius;
	private double defaultRadius;
	private LatLng currentPosition;
	private LatLng cameraLocation;
	private float cameraZoom;
	private int currentStatus;
	boolean isSearchAreaVisible = false;
	boolean isRadiusAreaVisible = false;
	boolean isOrientationChanged = false;
	boolean isWakingUp = false;
	boolean isFeetUnit = false;

	private final int STATUS_NONE = 1;
	private final int STATUS_ADD_LOCATION = 2;
	private final int STATUS_REMOVE_LOCATION = 3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.conditions_location);

		conditionsManager = new ConditionManager(
				ConditionsLocationActivity.this);
		Intent intent = getIntent();
		sitId = intent.getLongExtra("situationId", -1);
		title = getResources().getResourceEntryName(R.string.location);
		isUpdate = conditionsManager.hasThisCondition(title, sitId);

		geocoder = new Geocoder(this);
		pbLoading = (ProgressBar) findViewById(R.id.progress_bar_map);

		ibSearch = (ImageButton) findViewById(R.id.search_location_ib);
		ibRadius = (ImageButton) findViewById(R.id.set_radius_button);
		ibAddLocation = (ImageButton) findViewById(R.id.add_new_location_ib);
		ibRemoveLocation = (ImageButton) findViewById(R.id.remove_location_ib);
		search = (AutoCompleteTextView) findViewById(R.id.search_location);
		tvRadius = (TextView) findViewById(R.id.radius_tv);
		radiusSeekbar = (SeekBar) findViewById(R.id.radius_seekbar);

		setUpMapIfNeeded();

		SharedPreferences prefs = getSharedPreferences(C.prefs.PREFS_NAME,
				MODE_PRIVATE);
		defaultRadius = prefs.getInt(C.prefs.default_radius, 100);
		int radiusUnit = prefs.getInt(C.prefs.radius_unit, 0);
		if (radiusUnit == 0) {
			isFeetUnit = false;
		} else if (radiusUnit == 1) {
			isFeetUnit = true;
		}

		search.setAdapter(new AutoCompleteAdapter(
				ConditionsLocationActivity.this, geocoder));
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (isWakingUp) {
			isWakingUp = false;
		} else {
			if (isOrientationChanged) {
				// look at onRestoreInstanceState()
				if (location != null) {
					getAddressByLocation(currentPosition);
					drawMarkersAndCircles();

					if (locDrawables.size() != 0) {
						currentCircle = (Circle) locDrawables.get(
								currentPosition).get(keyCircle);
						currentMarker = (Marker) locDrawables.get(
								currentPosition).get(keyMarker);
						currentMarker
								.setIcon(BitmapDescriptorFactory
										.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
					}
					CameraUpdate zoomUpdate = CameraUpdateFactory
							.newLatLngZoom(cameraLocation, cameraZoom);
					mapView.animateCamera(zoomUpdate);
				}
			} else if (!isUpdate) {
				pbLoading.setVisibility(View.VISIBLE);
				getLocationFromListener();
				/*
				 * LocationManager locationManager = (LocationManager)
				 * getSystemService(Context.LOCATION_SERVICE);
				 * 
				 * // location = mapView.getMyLocation();
				 * if(locationManager.isProviderEnabled
				 * (LocationManager.GPS_PROVIDER)){ location =
				 * locationManager.getLastKnownLocation
				 * (LocationManager.GPS_PROVIDER); } else
				 * if(locationManager.isProviderEnabled
				 * (LocationManager.NETWORK_PROVIDER)){ location =
				 * locationManager
				 * .getLastKnownLocation(LocationManager.NETWORK_PROVIDER); }
				 * 
				 * if(location != null){ cameraLocation = new
				 * LatLng(location.getLatitude(), location.getLongitude());
				 * getAddressByLocation(cameraLocation); }else{ String
				 * coordinates[] = { "37.757992", "-122.388407" }; double lat =
				 * Double.parseDouble(coordinates[0]); double lng =
				 * Double.parseDouble(coordinates[1]); cameraLocation = new
				 * LatLng(lat, lng); }
				 */
				currentRadius = defaultRadius;
				currentStatus = STATUS_NONE;
			} else {
				desc = conditionsManager.getDescription(title, sitId);
				search.setText(desc);
				// set cursor on begin of search
				/*
				 * search.setSelection(1); search.extendSelection(0);
				 * search.setSelection(0);
				 */

				String note = conditionsManager.getNote(title, sitId);
				String parts[] = note.split(";");
				String locPart[] = parts[0].split(",");
				String radiusPart = parts[1];
				String camLocPart[] = parts[2].split(",");

				int start = radiusPart.indexOf("=");
				int end = radiusPart.indexOf("m");
				String radius = radiusPart.substring(start + 1, end);
				Log.e(TAG, "NOTE=" + locPart[0] + "," + locPart[1]);
				currentPosition = new LatLng(Double.valueOf(locPart[0]),
						Double.valueOf(locPart[1]));
				cameraLocation = new LatLng(Double.valueOf(camLocPart[0]),
						Double.valueOf(camLocPart[1]));
				currentRadius = Double.valueOf(radius);
				if (isFeetUnit) {
					currentRadius = Utils.meters2Feets(currentRadius);
				}
				currentStatus = STATUS_NONE;
				drawMarkersAndCircles();

				currentCircle = (Circle) locDrawables.get(currentPosition).get(
						keyCircle);
				currentMarker = (Marker) locDrawables.get(currentPosition).get(
						keyMarker);
				currentMarker.setIcon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

				CameraUpdate zoomUpdate = CameraUpdateFactory.newLatLngZoom(
						currentPosition, 17.0f);
				mapView.animateCamera(zoomUpdate);
			}
		}

		changeStatus(currentStatus);

		if (isSearchAreaVisible) {
			ibSearch.setBackgroundResource(R.drawable.btn_default_pressed_holo_light);
			search.setVisibility(View.VISIBLE);
		}

		ibSearch.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (search.getVisibility() == View.VISIBLE) {
					ibSearch.setBackgroundResource(R.drawable.btn_default_normal_holo_dark);
					search.setVisibility(View.GONE);
					isSearchAreaVisible = false;
				} else {
					ibSearch.setBackgroundResource(R.drawable.btn_default_pressed_holo_light);
					search.setVisibility(View.VISIBLE);
					isSearchAreaVisible = true;
				}
			}
		});
		search.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {

				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					desc = search.getText().toString();
					getLocationByAddress(desc);

					// --- Help for emulator bug
					/*
					 * String locationName = search.getText().toString();
					 * JSONObject obj = getLocationInfo(locationName); GeoPoint
					 * pt = getLatLong(obj); mc.animateTo(pt); mc.setZoom(19);
					 */
					// -------------
				}
				return false;
			}
		});

		if (isRadiusAreaVisible) {
			RelativeLayout radiusArea = (RelativeLayout) radiusSeekbar
					.getParent();
			radiusArea.setVisibility(View.VISIBLE);
			ibRadius.setBackgroundResource(R.drawable.btn_default_pressed_holo_light);
			refreshRadius();
		} else {
			ibRadius.setBackgroundResource(R.drawable.btn_default_normal_holo_dark);
		}

		radiusSeekbar
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					int fillColor = 0;
					float strokeWidth = 1f;

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						fillColor = currentCircle.getFillColor();
						strokeWidth = currentCircle.getStrokeWidth();
						currentCircle.setFillColor(Color.TRANSPARENT);
						currentCircle.setStrokeWidth(4.f);
					}

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						tvRadius.setText(String.valueOf(progress));
						currentRadius = progress;
						currentCircle.setRadius(progress);
						// refreshCurrentMarkerAndCircle(globalGeoPoint);
						LatLng northBound = Utils.destinationPoint(0,
								progress / 1000.0, currentCircle.getCenter());
						LatLng eastBound = Utils.destinationPoint(90,
								progress / 1000.0, currentCircle.getCenter());
						LatLng southBound = Utils.destinationPoint(180,
								progress / 1000.0, currentCircle.getCenter());
						LatLng westBound = Utils.destinationPoint(270,
								progress / 1000.0, currentCircle.getCenter());
						LatLngBounds.Builder bounds = new LatLngBounds.Builder();
						bounds.include(currentCircle.getCenter());
						bounds.include(eastBound);
						bounds.include(westBound);
						bounds.include(northBound);
						bounds.include(southBound);
						LatLngBounds update = bounds.build();
						mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(
								update, 100));
					}

					@Override
					public void onStopTrackingTouch(final SeekBar seekBar) {
						currentCircle.setStrokeWidth(strokeWidth);
						currentCircle.setFillColor(fillColor);

						refreshRadius(currentMarker.getPosition(),
								currentCircle.getRadius());
					}
				});

		ibRadius.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currentCircle == null) {
					Toast.makeText(ConditionsLocationActivity.this,
							R.string.choose_marker, Toast.LENGTH_LONG).show();
					return;
				}
				RelativeLayout radiusArea = (RelativeLayout) radiusSeekbar
						.getParent();
				if (radiusArea.getVisibility() != View.VISIBLE) {
					ibRadius.setBackgroundResource(R.drawable.btn_default_pressed_holo_light);
					radiusArea.setVisibility(View.VISIBLE);
					isRadiusAreaVisible = true;
					refreshRadius();
				} else {
					ibRadius.setBackgroundResource(R.drawable.btn_default_normal_holo_dark);
					radiusArea.setVisibility(View.GONE);
					isRadiusAreaVisible = false;
				}
			}
		});

		ibAddLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currentStatus != STATUS_ADD_LOCATION) {
					changeStatus(STATUS_ADD_LOCATION);
				} else {
					changeStatus(STATUS_NONE);
				}
			}
		});

		ibRemoveLocation.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (currentStatus != STATUS_REMOVE_LOCATION) {
					changeStatus(STATUS_REMOVE_LOCATION);
				} else {
					changeStatus(STATUS_NONE);
				}
			}
		});

	}

	public OnMapClickListener onMapClickListener = new OnMapClickListener() {

		@Override
		public void onMapClick(LatLng point) {
			if (currentStatus == STATUS_NONE) {
				currentPosition = point;
				getAddressByLocation(point);
				refreshPosition(currentMarker.getPosition(), point, search
						.getText().toString());
			} else if (currentStatus == STATUS_ADD_LOCATION) {
				currentPosition = point;
				currentRadius = defaultRadius;
				if (locDrawables.size() != 0) {
					currentMarker.setIcon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_RED));
				}

				int radius = (int) Math.round(currentRadius);
				if (isFeetUnit) {
					radius = (int) Math
							.round(Utils.feets2Meters(currentRadius));
				}

				getAddressByLocation(point);
				addMarkerAndCircle(point, radius);
				refreshRadius();

				CameraUpdate cameraUpdate = CameraUpdateFactory
						.newLatLng(point);
				mapView.animateCamera(cameraUpdate);
			} else if (currentStatus == STATUS_REMOVE_LOCATION) {
				Toast.makeText(ConditionsLocationActivity.this,
						getText(R.string.select_marker_to_delete),
						Toast.LENGTH_LONG).show();
			}
		}
	};

	public OnMarkerClickListener onMarkerClickListener = new OnMarkerClickListener() {

		@Override
		public boolean onMarkerClick(Marker marker) {
			LatLng markerPos = marker.getPosition();
			if (currentStatus == STATUS_REMOVE_LOCATION
					&& deleteMarkerAndCircle(marker)) {
				if (locDrawables.size() > 0
						&& currentMarker.getPosition().latitude == markerPos.latitude
						&& currentMarker.getPosition().longitude == markerPos.longitude) {
					currentPosition = locDrawables.entrySet().iterator().next()
							.getKey();
					currentMarker = (Marker) locDrawables.get(currentPosition)
							.get(keyMarker);
					currentCircle = (Circle) locDrawables.get(currentPosition)
							.get(keyCircle);
					currentMarker.setIcon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
					currentRadius = currentCircle.getRadius();
					refreshRadius();
					getAddressByLocation(currentPosition);
				}
				Toast.makeText(ConditionsLocationActivity.this,
						R.string.marker_removed, Toast.LENGTH_SHORT).show();
				return true;
			} else {
				changeCurrentMarkerAndCircle(marker);
				CameraUpdate cameraUpdate = CameraUpdateFactory
						.newLatLng(currentPosition);
				mapView.animateCamera(cameraUpdate);

				if (marker.isInfoWindowShown()) {
					marker.hideInfoWindow();
				} else {
					// marker.setTitle(getString(R.string.radius)+" "+markerRadius+unit);
					marker.showInfoWindow();
				}
			}
			return false;
		}
	};
	/*
	 * OnMarkerDragListener onMarkerDragListener = new
	 * GoogleMap.OnMarkerDragListener() { boolean isCurrent = false; LatLng
	 * startingPosition;
	 * 
	 * @Override public void onMarkerDragStart(Marker m) {
	 * if(!currentMarker.getPosition().equals(m.getPosition())){ isCurrent =
	 * false; } else { isCurrent = true; startingPosition = currentPosition;
	 * mapView.setOnMapClickListener(null);
	 * mapView.setOnCameraChangeListener(null); } }
	 * 
	 * @Override public void onMarkerDragEnd(Marker m) { if(isCurrent){
	 * currentPosition = m.getPosition(); getAddressByLocation(currentPosition);
	 * refreshPosition(startingPosition, currentPosition,
	 * search.getText().toString());
	 * mapView.setOnMapClickListener(onMapClickListener);
	 * mapView.setOnCameraChangeListener(onCameraChangeListener); } }
	 * 
	 * @Override public void onMarkerDrag(Marker m) { if(isCurrent){
	 * currentCircle.setCenter(m.getPosition()); } } };
	 */

	// when tap on my location button
	OnCameraChangeListener onCameraChangeListener = new GoogleMap.OnCameraChangeListener() {
		@Override
		public void onCameraChange(CameraPosition cameraPosition) {
			cameraLocation = cameraPosition.target;
			cameraZoom = cameraPosition.zoom;
			/*
			 * double latDelta = cameraPosition.target.latitude; double lonDelta
			 * = cameraPosition.target.longitude; Location l =
			 * mapView.getMyLocation(); if (l != null) { latDelta =
			 * Math.abs(latDelta - l.getLatitude()); lonDelta =
			 * Math.abs(lonDelta - l.getLongitude());
			 * 
			 * if (latDelta <= .000001 && lonDelta <= .000001) { location = l;
			 * cameraLocation = new LatLng(l.getLatitude(), l.getLongitude());
			 * 
			 * moveCurrentMarkerAndCircle(cameraLocation);
			 * getAddressByLocation(cameraLocation); } }
			 */
		}

	};

	OnMyLocationChangeListener onMyLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {

		@Override
		public void onMyLocationChange(Location l) {
			location = l;
		}
	};

	private void refreshRadius(LatLng position, double radius) {
		HashMap<String, Object> hm = locDrawables.get(position);
		if (hm != null) {
			long id = (Long) hm.get(keyId);
			((Circle) hm.get(keyCircle)).setRadius(radius);
			((Marker) hm.get(keyMarker)).setIcon(BitmapDescriptorFactory
					.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
			MarkerManager manager = new MarkerManager(this);
			manager.updateRadius(id, (int) Math.round(radius));
			manager.stop();
			return;
		}
	}

	private void changeCurrentMarkerAndCircle(Marker newMarker) {
		currentMarker.setIcon(BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_RED));
		currentMarker = newMarker;
		currentMarker.setIcon(BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
		currentPosition = newMarker.getPosition();
		HashMap<String, Object> hm = locDrawables.get(newMarker.getPosition());
		if (hm != null) {
			currentCircle = (Circle) hm.get(keyCircle);
		}

		refreshRadius();
		getAddressByLocation(currentPosition);
	}

	private void refreshPosition(LatLng from, LatLng to, String address) {
		HashMap<String, Object> hm = locDrawables.get(from);
		if (hm != null) {
			if (from.latitude == to.latitude && from.longitude == to.longitude) {
				CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(to);
				mapView.animateCamera(cameraUpdate);
				return;
			}

			Circle c = (Circle) hm.get(keyCircle);
			Marker m = (Marker) hm.get(keyMarker);
			long id = (Long) hm.get(keyId);

			MarkerManager manager = new MarkerManager(this);
			manager.updateMarker(id, sitId, to.latitude, to.longitude,
					(int) Math.round(c.getRadius()), search.getText()
							.toString());
			manager.stop();

			locDrawables.remove(from);
			m.setPosition(to);
			c.setCenter(to);

			HashMap<String, Object> data = new HashMap<String, Object>();
			data.put(keyId, id);
			data.put(keyMarker, m);
			data.put(keyCircle, c);
			locDrawables.put(to, data);
		}

		CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(to);
		mapView.animateCamera(cameraUpdate);
	}

	private void drawMarkersAndCircles() {
		locDrawables = new HashMap<LatLng, HashMap<String, Object>>();

		MarkerManager mm = new MarkerManager(ConditionsLocationActivity.this);
		Cursor c = mm.getAllLocationsForSituation(sitId);

		final int idIndex = c.getColumnIndex(TABLE_LOCATION.ID);
		final int latIndex = c.getColumnIndex(TABLE_LOCATION.LATITUDE);
		final int lonIndex = c.getColumnIndex(TABLE_LOCATION.LONGITUDE);
		final int radiusIndex = c.getColumnIndex(TABLE_LOCATION.RADIUS);

		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			long id = c.getLong(idIndex);
			double lat = c.getDouble(latIndex);
			double lon = c.getDouble(lonIndex);
			Log.e(TAG, "DB = " + lat + "," + lon);
			int radius = c.getInt(radiusIndex);

			LatLng point = new LatLng(lat, lon);
			MarkerOptions markerOptions = new MarkerOptions();
			markerOptions.position(point);
			markerOptions.icon(BitmapDescriptorFactory
					.defaultMarker(BitmapDescriptorFactory.HUE_RED));
			// markerOptions.draggable(true);

			CircleOptions circleOptions = new CircleOptions();
			circleOptions.center(point);
			circleOptions.radius(radius);
			circleOptions.fillColor(Color.argb(128, 0, 128, 255));
			circleOptions.strokeColor(Color.BLUE);
			circleOptions.strokeWidth(1f);

			Marker marker = mapView.addMarker(markerOptions);
			Circle circle = mapView.addCircle(circleOptions);

			HashMap<String, Object> hm = new HashMap<String, Object>();
			hm.put(keyId, id);
			hm.put(keyMarker, marker);
			hm.put(keyCircle, circle);
			locDrawables.put(point, hm);
		}
		c.close();
		mm.stop();
	}

	private void addMarkerAndCircle(LatLng point, double radiusInMeters) {
		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position(point);
		markerOptions.icon(BitmapDescriptorFactory
				.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
		// markerOptions.draggable(true);

		CircleOptions circleOptions = new CircleOptions();
		circleOptions.center(point);
		circleOptions.radius(radiusInMeters);
		circleOptions.fillColor(Color.argb(128, 0, 128, 255));
		circleOptions.strokeColor(Color.BLUE);
		circleOptions.strokeWidth(1f);

		currentMarker = mapView.addMarker(markerOptions);
		currentCircle = mapView.addCircle(circleOptions);
		currentPosition = currentMarker.getPosition();

		MarkerManager mm = new MarkerManager(ConditionsLocationActivity.this);
		long locId = mm.addLocation(sitId, currentPosition.latitude,
				currentPosition.longitude, (int) Math.round(radiusInMeters),
				search.getText().toString());
		mm.stop();
		HashMap<String, Object> newData = new HashMap<String, Object>();
		newData.put(keyId, locId);
		newData.put(keyMarker, currentMarker);
		newData.put(keyCircle, currentCircle);

		if (locDrawables == null) {
			locDrawables = new HashMap<LatLng, HashMap<String, Object>>();
		}

		locDrawables.put(currentMarker.getPosition(), newData);

	}

	private boolean deleteMarkerAndCircle(Marker m) {
		HashMap<String, Object> data = locDrawables.get(m.getPosition());
		if (data == null) {
			Toast.makeText(ConditionsLocationActivity.this,
					"Marker NOT deleted!!!", Toast.LENGTH_SHORT).show();
			return false;
		}

		Circle circle = (Circle) data.get(keyCircle);
		long id = (Long) data.get(keyId);

		locDrawables.remove(m.getPosition());

		circle.remove();
		m.remove();

		MarkerManager manager = new MarkerManager(
				ConditionsLocationActivity.this);
		int deleted = manager.deleteLocation(sitId, id);
		manager.stop();

		if (locDrawables.size() == 0) {
			currentMarker = null;
			currentCircle = null;

			if (isRadiusAreaVisible) {
				RelativeLayout radiusArea = (RelativeLayout) radiusSeekbar
						.getParent();
				ibRadius.setBackgroundResource(R.drawable.btn_default_normal_holo_dark);
				radiusArea.setVisibility(View.GONE);
				isRadiusAreaVisible = false;
			}
		}

		return deleted > 0 ? true : false;
	}

	private void refreshRadius() {
		int radius = (int) Math.round(currentRadius);

		tvRadius.setText(String.valueOf(radius));
		radiusSeekbar.setProgress(radius);
	}

	protected void getLocationFromListener() {
		Intent intentToFire = new Intent(
				LocationReceiver.ACTION_REFRESH_SCHEDULE_ALARM);
		sendBroadcast(intentToFire);

		OnNewLocationListener onNewLocationListener = new OnNewLocationListener() {
			@Override
			public void onNewLocationReceived(Location loc) {
				location = loc;
				pbLoading.setVisibility(View.GONE);
				cameraLocation = new LatLng(location.getLatitude(),
						location.getLongitude());
				getAddressByLocation(cameraLocation);

				addMarkerAndCircle(cameraLocation, currentRadius);
				CameraUpdate zoomUpdate = CameraUpdateFactory.newLatLngZoom(
						currentPosition, 17.0f);
				mapView.animateCamera(zoomUpdate);

				// then stop listening
				LocationReceiver.clearOnNewLocationListener(this);
			}
		};

		// start listening for new location
		LocationReceiver.setOnNewLocationListener(onNewLocationListener);
	}

	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mapView == null) {
			MapsInitializer.initialize(this);

			// Try to obtain the map from the SupportMapFragment.
			mapView = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.mapview)).getMap();

			if (isGoogleMapsInstalled()) {
				if (mapView != null) {
					mapView.setMapType(GoogleMap.MAP_TYPE_NORMAL);
					mapView.setMyLocationEnabled(true);
					mapView.setOnMapClickListener(onMapClickListener);
					mapView.setOnMarkerClickListener(onMarkerClickListener);
					// mapView.setOnMarkerDragListener(onMarkerDragListener);
					mapView.setOnCameraChangeListener(onCameraChangeListener);
					mapView.setOnMyLocationChangeListener(onMyLocationChangeListener);
				}
			} else {
				Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Install Google Maps");
				builder.setCancelable(false);
				builder.setPositiveButton("Install", getGoogleMapsListener());
				AlertDialog dialog = builder.create();
				dialog.show();
			}
		}
	}

	private void changeStatus(int status) {
		ibAddLocation
				.setBackgroundResource(R.drawable.btn_default_normal_holo_dark);
		ibRemoveLocation
				.setBackgroundResource(R.drawable.btn_default_normal_holo_dark);

		switch (status) {
		case STATUS_ADD_LOCATION:
			ibAddLocation
					.setBackgroundResource(R.drawable.btn_default_pressed_holo_light);
			break;
		case STATUS_REMOVE_LOCATION:
			ibRemoveLocation
					.setBackgroundResource(R.drawable.btn_default_pressed_holo_light);
			break;
		case STATUS_NONE:

			break;
		}

		currentStatus = status;
	}

	public void onChangeModeView(View v) {
		int type = mapView.getMapType();

		Dialog dialog = onCreateDialogSingleChoice(type);
		dialog.show();
	}

	public Dialog onCreateDialogSingleChoice(int type) {
		int selected = -1;

		switch (type) {
		case GoogleMap.MAP_TYPE_NORMAL:
			selected = 0;
			break;
		case GoogleMap.MAP_TYPE_SATELLITE:
			selected = 1;
			break;
		case GoogleMap.MAP_TYPE_HYBRID:
			selected = 2;
			break;
		case GoogleMap.MAP_TYPE_TERRAIN:
			selected = 3;
			break;
		}

		CharSequence[] array = getResources().getTextArray(
				R.array.map_types_array);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getText(R.string.select_map_type))
		// Specify the list array, the items to be selected by default (null for
		// none),
		// and the listener through which to receive callbacks when items are
		// selected
				.setSingleChoiceItems(array, selected,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								switch (which) {
								case 0:
									mapView.setMapType(GoogleMap.MAP_TYPE_NORMAL);
									break;
								case 1:
									mapView.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
									break;
								case 2:
									mapView.setMapType(GoogleMap.MAP_TYPE_HYBRID);
									break;
								case 3:
									mapView.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
									break;
								}

							}
						});
		builder.setPositiveButton(getText(android.R.string.ok),
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});

		return builder.create();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_location, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i = null;

		switch (item.getItemId()) {

		// case R.id.menu_help: i = new Intent(this, HelpActivity.class);
		// startActivity(i); break;

		case R.id.menu_dont_save:
			setResult(Activity.RESULT_OK, i);
			finish();
			break;
		case R.id.menu_save:
			setResult(Activity.RESULT_CANCELED, i);
			finish();
			break;
		}

		return false;
	}

	public void getLocationByAddress(final String myAddress) {
		pbLoading.setVisibility(View.VISIBLE);

		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					List<Address> addressList = geocoder.getFromLocationName(
							myAddress, 5);// thread
					if (addressList != null && addressList.size() > 0) {
						double lat = addressList.get(0).getLatitude();
						double lng = addressList.get(0).getLongitude();
						cameraLocation = new LatLng(lat, lng);

					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		pbLoading.post(new Runnable() {
			@Override
			public void run() {
				pbLoading.setVisibility(View.GONE);

				CameraUpdate positionUpdate = CameraUpdateFactory
						.newLatLng(cameraLocation);
				mapView.animateCamera(positionUpdate);
			}
		});

		new Thread(runnable).start();
	}

	public void getAddressByLocation(final LatLng point) {
		// pbLoading.setVisibility(View.VISIBLE);
		// Get the address by coordinates
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					String address = "";
					List<Address> addresses = geocoder.getFromLocation(
							point.latitude, point.longitude, 1);

					if (addresses.size() > 0) {
						for (int i = 0; i < addresses.get(0)
								.getMaxAddressLineIndex(); i++) {
							address += addresses.get(0).getAddressLine(i) + " ";
						}
					}
					// get the new address
					desc = address;
				} catch (IOException e) {
					e.printStackTrace();
				}
				pbLoading.post(new Runnable() {
					@Override
					public void run() {
						// pbLoading.setVisibility(View.GONE);
						search.setText(desc);
					}
				});

			}

		};
		new Thread(runnable).start();
	}

	public boolean isGoogleMapsInstalled() {
		try {
			ApplicationInfo info = getPackageManager().getApplicationInfo(
					"com.google.android.apps.maps", 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	public DialogInterface.OnClickListener getGoogleMapsListener() {
		return new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent(
						Intent.ACTION_VIEW,
						Uri.parse("market://details?id=com.google.android.apps.maps"));
				startActivity(intent);

				// Finish the activity so they can't circumvent the check
				finish();
			}
		};
	}

	private void hideKeyboard(EditText et) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
	}

	/*
	 * public int metersToRadius(float meters, GoogleMap map, double latitude) {
	 * return (int) (map.getProjection().metersToEquatorPixels(meters) * (1/
	 * Math.cos(Math.toRadians(latitude)))); }
	 */

	// --- HELP for emulator bug ---
	/*
	 * public static JSONObject getLocationInfo(String address) { StringBuilder
	 * stringBuilder = new StringBuilder(); try {
	 * 
	 * address = address.replaceAll(" ","%20");
	 * 
	 * HttpPost httppost = new
	 * HttpPost("http://maps.google.com/maps/api/geocode/json?address=" +
	 * address + "&sensor=false"); HttpClient client = new DefaultHttpClient();
	 * HttpResponse response; stringBuilder = new StringBuilder();
	 * 
	 * 
	 * response = client.execute(httppost); HttpEntity entity =
	 * response.getEntity(); InputStream stream = entity.getContent(); int b;
	 * while ((b = stream.read()) != -1) { stringBuilder.append((char) b); } }
	 * catch (ClientProtocolException e) { } catch (IOException e) { }
	 * 
	 * JSONObject jsonObject = new JSONObject(); try { jsonObject = new
	 * JSONObject(stringBuilder.toString()); } catch (JSONException e) {
	 * e.printStackTrace(); }
	 * 
	 * return jsonObject; }
	 * 
	 * public static GeoPoint getLatLong(JSONObject jsonObject) {
	 * 
	 * Double lon = new Double(0); Double lat = new Double(0);
	 * 
	 * try {
	 * 
	 * lon = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
	 * .getJSONObject("geometry").getJSONObject("location") .getDouble("lng");
	 * 
	 * lat = ((JSONArray)jsonObject.get("results")).getJSONObject(0)
	 * .getJSONObject("geometry").getJSONObject("location") .getDouble("lat");
	 * 
	 * } catch (Exception e) { e.printStackTrace();
	 * 
	 * }
	 * 
	 * return new GeoPoint((int) (lat * 1E6), (int) (lon * 1E6)); }
	 */
	// --------------

	@Override
	public void onBackPressed() {
		if (locDrawables.size() == 0) {
			AlertDialog.Builder builder = new AlertDialog.Builder(
					ConditionsLocationActivity.this);
			builder.setTitle(R.string.add_location);
			builder.setMessage(R.string.one_atleast);
			// Add the buttons
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
						}
					});
			builder.setNegativeButton(R.string.remove,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							setResult(Activity.RESULT_CANCELED, getIntent());
							finish();
						}
					});

			// 3. Get the AlertDialog from create()
			AlertDialog dialog = builder.create();
			dialog.show();
			return;

		} else {
			int radius = (int) Math.round(currentRadius);
			if (isFeetUnit) {
				radius = (int) Math.round(Utils.feets2Meters(currentRadius));
			}

			String note = currentPosition.latitude + ","
					+ currentPosition.longitude + ";R=" + radius + "m;"
					+ cameraLocation.latitude + "," + cameraLocation.longitude;

			desc = search.getText().toString();

			if (!isUpdate) {
				conditionsManager.addCondition(title, sitId, desc, note);
			} else {
				conditionsManager.updateCondition(title, sitId, desc, note);
			}

			setResult(Activity.RESULT_OK, getIntent());
			finish();
		}
	}

	@Override
	protected void onDestroy() {
		conditionsManager.stop();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		isWakingUp = true;
	}

	private final String saveProvider = "provider";
	private final String saveAccuracy = "accuracy";
	private final String saveLatitude = "my_latitude";
	private final String saveLongitude = "my_longitude";
	private final String saveTimeFix = "time_fix";
	private final String saveStatus = "status";
	private final String saveRadius = "radius";
	private final String saveCameraLat = "camera_latitude";
	private final String saveCameraLon = "camera_longitude";
	private final String saveZoom = "camera_zoom";
	private final String saveMarkerLat = "marker_latitude";
	private final String saveMarkerLon = "marker_longitude";
	private final String saveSearchVisibility = "search_visibility";
	private final String saveRadiusVisibility = "radius_visibility";

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (location != null) {
			outState.putString(saveProvider, location.getProvider());
			outState.putFloat(saveAccuracy, location.getAccuracy());
			outState.putDouble(saveLatitude, location.getLatitude());
			outState.putDouble(saveLongitude, location.getLongitude());
			outState.putLong(saveTimeFix, location.getTime());
		}
		outState.putInt(saveStatus, currentStatus);
		outState.putDouble(saveRadius, currentRadius);

		outState.putDouble(saveCameraLat, cameraLocation.latitude);
		outState.putDouble(saveCameraLon, cameraLocation.longitude);
		outState.putFloat(saveZoom, cameraZoom);

		if (currentMarker != null) {
			double lastMarkerLat = currentMarker.getPosition().latitude;
			double lastMarkerLon = currentMarker.getPosition().longitude;

			outState.putDouble(saveMarkerLat, lastMarkerLat);
			outState.putDouble(saveMarkerLon, lastMarkerLon);
		}

		outState.putBoolean(saveSearchVisibility, isSearchAreaVisible);
		outState.putBoolean(saveRadiusVisibility, isRadiusAreaVisible);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		String provider = savedInstanceState.getString(saveProvider);
		float accuracy = savedInstanceState.getFloat(saveAccuracy, 0f);
		double latitude = savedInstanceState.getDouble(saveLatitude);
		double longitude = savedInstanceState.getDouble(saveLongitude);
		long timeFix = savedInstanceState.getLong(saveTimeFix);
		isSearchAreaVisible = savedInstanceState.getBoolean(
				saveSearchVisibility, false);
		isRadiusAreaVisible = savedInstanceState.getBoolean(
				saveRadiusVisibility, false);
		currentStatus = savedInstanceState.getInt(saveStatus);
		currentRadius = savedInstanceState.getDouble(saveRadius);

		double camLat = savedInstanceState.getDouble(saveCameraLat, latitude);
		double camLon = savedInstanceState.getDouble(saveCameraLon, longitude);
		cameraLocation = new LatLng(camLat, camLon);
		cameraZoom = savedInstanceState.getFloat(saveZoom, 17f);

		double markerLat = savedInstanceState
				.getDouble(saveMarkerLat, latitude);
		double markerLon = savedInstanceState.getDouble(saveMarkerLon,
				longitude);
		currentPosition = new LatLng(markerLat, markerLon);

		location = new Location(provider);
		location.setAccuracy(accuracy);
		location.setLatitude(latitude);
		location.setLongitude(longitude);
		location.setTime(timeFix);

		isOrientationChanged = true;
	}
}
