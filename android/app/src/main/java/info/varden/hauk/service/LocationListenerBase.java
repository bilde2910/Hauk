package info.varden.hauk.service;

import android.location.LocationListener;
import android.os.Bundle;

/**
 * Location listener base class for Hauk. The purpose of this class is to remove unnecessary empty
 * function bodies from LocationPushService's source code.
 *
 * @author Marius Lindvall
 */
abstract class LocationListenerBase implements LocationListener {
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }
}
