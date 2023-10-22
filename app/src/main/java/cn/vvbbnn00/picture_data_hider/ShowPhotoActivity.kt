package cn.vvbbnn00.picture_data_hider

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import cn.vvbbnn00.picture_data_hider.utils.SQLiteHelper
import cn.vvbbnn00.picture_data_hider.models.Photo
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat

class ShowPhotoActivity : AppCompatActivity() {
    private val TAG = "ShowPhotoActivity"
    private var photo: Photo? = null

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_photo)

        val photoId = intent.getIntExtra("photoId", -1)

        if (photoId == -1) {
            Log.d(TAG, "onCreate: photoId is -1")
            finish()
        }

        photo = SQLiteHelper(this).query(photoId)
        if (photo == null) {
            Log.d(TAG, "onCreate: photo is null")
            finish()
        }

        Glide
            .with(this)
            .load(photo!!.path)
            .into(findViewById(R.id.iv_photo))

        findViewById<TextView>(R.id.sp_tv_title).text =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(photo!!.timestamp)
        findViewById<TextView>(R.id.sp_tv_description).text =
            "Latitude: ${photo!!.latitude} Longitude: ${photo!!.longitude}"

        findViewById<Button>(R.id.btn_delete).setOnClickListener() {
            confirmDelete()
        }
    }


    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Are you sure to delete this photo?")
            .setPositiveButton("Yes") { _, _ ->
                SQLiteHelper(this).delete(photo!!.id)
                finish()
            }
            .setNegativeButton("No") { _, _ ->
                // Do nothing
            }
            .create()
            .show()
    }
}