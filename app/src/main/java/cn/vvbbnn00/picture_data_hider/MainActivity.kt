package cn.vvbbnn00.picture_data_hider

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission();

        findViewById<Button>(R.id.btn_take_photo).setOnClickListener() {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_decrypt_photo).setOnClickListener() {
            val intent = Intent(this, PhotoDecryptionActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_albumn).setOnClickListener() {
            val intent = Intent(this, AlbumActivity::class.java)
            startActivity(intent)
        }
    }

}