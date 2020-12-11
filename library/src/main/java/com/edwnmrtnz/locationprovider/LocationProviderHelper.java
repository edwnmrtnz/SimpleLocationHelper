package com.edwnmrtnz.locationprovider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.edwnmrtnz.locationprovider.callback.OnLocationReceiver;
import com.edwnmrtnz.locationprovider.enums.LocationUpdateStatus;
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


public class LocationProviderHelper {
    private static final String TAG = "LocationProviderHelper";

    private static LocationProviderHelper INSTANCE = null;

    private Context context;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private final FusedLocationProviderClient fusedLocationProviderClient;
    private final SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;

    private OnLocationReceiver onLocationReceiver;
    private Location bestLastKnownLocation;
    private final int PRIORITY;


    public LocationProviderHelper(Context context, int priority) {

        this.context = context;

        this.PRIORITY = priority;

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);

        settingsClient = LocationServices.getSettingsClient(context);

        createLocationRequest();

        createLocationCallback();

        buildLocationSettingsRequest();
    }

    public static LocationProviderHelper getInstance(Context context) {
        if (context == null) throw new NullPointerException("Context cannot be null");
        if (INSTANCE == null) INSTANCE = new LocationProviderHelper(
                context,
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        );
        return INSTANCE;
    }

    public static LocationProviderHelper getInstance(Context context, int priority) {
        if (context == null) throw new NullPointerException("Context cannot be null");
        if (INSTANCE == null) INSTANCE = new LocationProviderHelper(
                context,
                priority
        );
        return INSTANCE;
    }

    public void setOnLocationReceiver(OnLocationReceiver onLocationReceiver) {
        this.onLocationReceiver = onLocationReceiver;
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();

        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        locationRequest.setPriority(PRIORITY);
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
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener((Activity) context, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            if (onLocationReceiver != null)
                                onLocationReceiver.onFailed(LocationUpdateStatus.NO_PERMISSION);
                            return;
                        }
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        if (onLocationReceiver != null)
                            onLocationReceiver.onLocationReceiverStarted();

                    }
                }).addOnFailureListener((Activity) context, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
                        Log.e(TAG, "Device has problem that can be solved by following the message in the exception: "
                                + ((ApiException) e).getStatus().getStatusMessage());
                        if (onLocationReceiver != null)
                            onLocationReceiver.onResolutionRequired(e);
                        break;
                    }
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
                        Log.e(TAG, "Device has problem that can't be solved");
                        if (onLocationReceiver != null)
                            onLocationReceiver.onFailed(LocationUpdateStatus.NOT_RESOLVABLE);
                        break;
                    }
                }
            }
        });
    }

    private void sendUpdates() {
        if (onLocationReceiver != null)
            onLocationReceiver.onLocationAcquired(bestLastKnownLocation, bestLastKnownLocation.getAccuracy());
    }

    public void stopLocationUpdates() {
        fusedLocationProviderClient
                .removeLocationUpdates(locationCallback)
                .addOnCompleteListener((Activity) context, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.e(TAG, "On Location updates has been stop via stopLocationUpdates()");
                    }
                });
    }

    public void destroyInstance() {
        onLocationReceiver = null;
        context = null;
        INSTANCE = null;
    }

    public boolean isLocationEnabled() {
        LocationManager location = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return location.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
