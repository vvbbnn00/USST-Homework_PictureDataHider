package cn.vvbbnn00.picture_data_hider

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.util.logging.Logger


class LocationService : Service() {
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    inner class LocationBinder : Binder() {
        fun getService(): LocationService = this@LocationService
    }

    private var currentLocation: Location? = null

    fun getCurrentLocation(): Location? {
        return currentLocation
    }


    override fun onCreate() {
        super.onCreate()

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createLocationCallback()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 1000  // 1 second
            fastestInterval = 500  // 0.5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0 ?: return
                currentLocation = p0.lastLocation
                Logger.getLogger("LocationService").info("Location updated: $currentLocation")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } else {
            Logger.getLogger("LocationService").warning("No permission")
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder {
        return LocationBinder()
    }
}
