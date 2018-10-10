package com.edwnmrtnz.locationprovider.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

public class GpsStatusChangeReceiver extends BroadcastReceiver {

    private OnGpsLocationSettingsChanged onGpsLocationSettingsChanged;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            if(onGpsLocationSettingsChanged != null){
                onGpsLocationSettingsChanged.onGpsSettingsChanged();
            }
        }
    }

    public void setOnGpsLocationSettingsChanged(OnGpsLocationSettingsChanged onGpsLocationSettingsChanged) {
        this.onGpsLocationSettingsChanged = onGpsLocationSettingsChanged;
    }

    public interface OnGpsLocationSettingsChanged {
        void onGpsSettingsChanged();
    }
}