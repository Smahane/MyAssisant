package com.phonesettings.myassistant.services;

import android.location.Location;

public interface OnNewLocationListener {
    public abstract void onNewLocationReceived(Location location);

}
