package com.edwnmrtnz.simplelocationhelper

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.edwnmrtnz.locationprovider.LocationProviderHelper
import com.edwnmrtnz.locationprovider.callback.OnLocationReceiver
import com.edwnmrtnz.locationprovider.constant.LocationProvideRequest
import com.edwnmrtnz.locationprovider.enums.LocationUpdateStatus
import com.google.android.gms.common.api.ResolvableApiException

class MainActivity : AppCompatActivity(), OnLocationReceiver {

    private var hasNotResolvableProblem = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()

        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()

        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        LocationProviderHelper.getInstance(this).startLocationUpdates()
        LocationProviderHelper.getInstance(this).setOnLocationReceiver(this)
    }

    private fun stopLocationUpdates() {
        LocationProviderHelper.getInstance(this).stopLocationUpdates()
    }


    override fun onLocationReceiverStarted() {
        Log.e(TAG, "onLocationReceiverStarted")
    }

    override fun onLocationAcquired(location: Location, accuracy: Float) {
        val updates = "Location: " + location.latitude + ":" + location.longitude + " - " + accuracy + "%"
        Toast.makeText(this, updates, Toast.LENGTH_SHORT).show()
    }

    override fun onResolutionRequired(e: Exception?) {
        try {
            val rae = e as ResolvableApiException
            rae.startResolutionForResult(this, LocationProvideRequest.REQUEST_CHECK_SETTINGS)
        } catch (sie: IntentSender.SendIntentException) {
            Log.e(TAG, "PendingIntent unable to execute request.")
            hasNotResolvableProblem = true
        }
    }

    override fun onFailed(locationFinderProblem: LocationUpdateStatus?) {
        if (locationFinderProblem === LocationUpdateStatus.NO_PERMISSION) Toast.makeText(this, "Please request permission!", Toast.LENGTH_SHORT).show()
        if (locationFinderProblem === LocationUpdateStatus.NOT_RESOLVABLE) hasNotResolvableProblem = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            LocationProvideRequest.REQUEST_CHECK_SETTINGS -> {
                if(resultCode == Activity.RESULT_OK){
                    Log.e(TAG, "User agreed to make required location settings changes.")
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "Please allow request!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    companion object {
        const val TAG = "LocationHelper"
    }
}
