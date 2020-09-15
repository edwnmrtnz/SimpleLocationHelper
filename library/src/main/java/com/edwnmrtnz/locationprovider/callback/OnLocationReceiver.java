package com.edwnmrtnz.locationprovider.callback;

import android.location.Location;

import com.edwnmrtnz.locationprovider.enums.LocationUpdateStatus;

public interface OnLocationReceiver {

    void onLocationReceiverStarted();

    void onLocationAcquired(Location location, float accuracy);

    void onResolutionRequired(Exception e);

    void onFailed(LocationUpdateStatus locationFinderProblem);

}
