package com.edwnmrtnz.locationprovider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.edwnmrtnz.locationprovider.callback.OnLocationReceiver;
import com.edwnmrtnz.locationprovider.enums.LocationUpdateStatus;
import com.edwnmrtnz.locationprovider.receiver.GpsStatusChangeReceiver;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


public class LocationProviderHelper implements GpsStatusChangeReceiver.OnGpsLocationSettingsChanged {
    private static final String TAG = "LocationProviderHelper";

    private static LocationProviderHelper INSTANCE = null;

    private Context context;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;

    private OnLocationReceiver onLocationReceiver;
    private Location bestLastKnownLocation;

    private GpsStatusChangeReceiver gpsStatusChangeReceiver;

    public LocationProviderHelper(Context context) {
        this.context = context;

        gpsStatusChangeReceiver = new GpsStatusChangeReceiver();

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        settingsClient = LocationServices.getSettingsClient(context);

        createLocationRequest();

        createLocationCallback();

        buildLocationSettingsRequest();
    }

    public static LocationProviderHelper getInstance(Context context) {
        if(context == null) throw new NullPointerException("Context cannot be null");
        if(INSTANCE == null) INSTANCE = new LocationProviderHelper(context);
        return INSTANCE;
    }

    public void setOnLocationReceiver(OnLocationReceiver onLocationReceiver) {
        this.onLocationReceiver = onLocationReceiver;
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.e(TAG, "onLocationResult: " + locationResult.toString());
                bestLastKnownLocation = locationResult.getLastLocation();
                if (onLocationReceiver != null) {
                    sendUpdates();
                }
                super.onLocationResult(locationResult);
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    public void startLocationUpdates() {

        startGpsTracking();

        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener((Activity) context, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                        Log.e(TAG, "Should start location update");

                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            Log.e(TAG, "Permission not permitted");
                            if(onLocationReceiver != null) onLocationReceiver.onFailed(LocationUpdateStatus.NO_PERMISSION);
                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if(location != null) {
                                    Log.e(TAG, "Last known location: " + location.toString());
                                }
                            }
                        });
                        if(onLocationReceiver != null) onLocationReceiver.onLocationReceiverStarted();

                    }
                }).addOnFailureListener((Activity) context, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:{
                        Log.e(TAG, "Device has problem that can be solved by following the message in the exception: " + e.getMessage());
                        if(onLocationReceiver != null) onLocationReceiver.onResolutionRequired(e);
                        break;
                    }
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:{
                        Log.e(TAG, "Device has problem that can't be solved");
                        if(onLocationReceiver != null) onLocationReceiver.onFailed(LocationUpdateStatus.NOT_RESOLVABLE);
                        break;
                    }
                }
            }
        });
    }

    private void startGpsTracking() {
        context.registerReceiver(gpsStatusChangeReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        gpsStatusChangeReceiver.setOnGpsLocationSettingsChanged(this);
    }

    private void stopGpsTracking() {
        context.unregisterReceiver(gpsStatusChangeReceiver);
    }

    private void sendUpdates() {
        if(onLocationReceiver != null) onLocationReceiver.onLocationAcquired(bestLastKnownLocation, bestLastKnownLocation.getAccuracy());
    }

    public void stopLocationUpdates() {

        stopGpsTracking();

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener((Activity)context, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.e(TAG, "On Location updates has been stop via stopLocationUpdates()");
                    }
                });
    }

    public boolean isLocationEnabled() {
        LocationManager location = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return location.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onGpsSettingsChanged() {
        if(!isLocationEnabled()) {
            if(onLocationReceiver != null) onLocationReceiver.onFailed(LocationUpdateStatus.GPS_NOT_OPEN);
            startLocationUpdates();
        }
    }
}
