package cn.vvbbnn00.picture_data_hider

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.gms.location.FusedLocationProviderClient

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var locationService: LocationService? = null
    private var isBound = false


    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Ask for permission
            AlertDialog.Builder(this)
                .setTitle("Permission required")
                .setMessage("Location permission is required for this app to work")
                .setPositiveButton("OK") { _, _ ->
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        0
                    )
                }
                .setNegativeButton("Cancel") { _, _ ->
                    // Do nothing
                }
                .create()
                .show()
        }
    }


    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // Here, you can interact with the service's binder, if available.
            val binder = service as LocationService.LocationBinder
            locationService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // Handle the disconnection here.
            isBound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission();

        // GPS定位
        val intent = Intent(this, LocationService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)

        findViewById<Button>(R.id.btn_location).setOnClickListener() {
            val location = locationService?.getCurrentLocation()
            if (location != null) {
                val msg = "Location: ${location.latitude}, ${location.longitude}"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location is null", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btn_take_photo).setOnClickListener() {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_decrypt_photo).setOnClickListener() {
            val intent = Intent(this, PhotoDecryptionActivity::class.java)
            startActivity(intent)
        }
    }

}