# SimpleLocationHelper

Get the device location without worrying how it's done!

[![](https://jitpack.io/v/edwnmrtnz/SimpleLocationHelper.svg)](https://jitpack.io/#edwnmrtnz/SimpleLocationHelper)

### Prerequisites
```gradle
allprojects {
    repositories {
	    ...
	    maven { url 'https://jitpack.io' }
	}
}
```
### Dependency
Add this to your module's build.gradle file (make sure the version matches the JitPack badge above):
```gradle
dependencies {
  implementation 'com.github.edwnmrtnz:SimpleLocationHelper:v1.0'
}
```

### Usage

Implement the interface of the Location Reciever

```kotlin
  class MainActivity : AppCompatActivity(), OnLocationReceiver {
 
  override fun onLocationReceiverStarted() {
        Log.e(TAG, "onLocationReceiverStarted")
    }

    override fun onLocationAcquired(location: Location, accuracy: Float) {
        val updates = "Location: " + location.latitude + ":" + 
                       location.longitude + " - " + accuracy + "%"
        Toast.makeText(this, updates, Toast.LENGTH_SHORT).show();
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
        if (locationFinderProblem === LocationUpdateStatus.NO_PERMISSION) 
          Toast.makeText(this, "Please request permission!", Toast.LENGTH_SHORT).show();
        if (locationFinderProblem === LocationUpdateStatus.GPS_NOT_OPEN) 
          Toast.makeText(this, "Please open your gps to start receiving location updates!", Toast.LENGTH_SHORT).show()
        if (locationFinderProblem === LocationUpdateStatus.NOT_RESOLVABLE) 
          hasNotResolvableProblem = true
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
  }
```
then call the necessary methods with respect to your lifecycle.

```kotlin
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
```

### Note
No need to worry about the device location being off intentionaly.

