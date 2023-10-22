package cn.vvbbnn00.picture_data_hider

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import cn.vvbbnn00.picture_data_hider.databinding.ActivityCameraBinding
import cn.vvbbnn00.picture_data_hider.models.Photo
import cn.vvbbnn00.picture_data_hider.utils.AESHelper
import cn.vvbbnn00.picture_data_hider.utils.FFTWatermarkHelper
import cn.vvbbnn00.picture_data_hider.utils.SQLiteHelper
import org.json.JSONObject
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * the cameraX code is referenced from https://developer.android.com/codelabs/camerax-getting-started#4
 */
class CameraActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var locationService: LocationService? = null
    private var isBound = false

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                }

                else -> {
                    super.onManagerConnected(status)
                }
            }
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

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up GPS service
        val intent = Intent(this, LocationService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        startService(intent)
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.CHINA)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PictureDataHider")
            }
        }

        val realSavePath = "/storage/emulated/0/Pictures/PictureDataHider/$name.jpg"

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: return
                    val hiddenDataPath = addHiddenData(realSavePath, this@CameraActivity)

                    val msg = "Photo saved into album."
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()

                    SQLiteHelper(this@CameraActivity).insert(
                        Photo(
                            0,
                            hiddenDataPath,
                            System.currentTimeMillis(),
                            locationService?.getCurrentLocation()?.latitude ?: 0.0,
                            locationService?.getCurrentLocation()?.longitude ?: 0.0,
                            0
                        )
                    )

                    Log.d(TAG, "$savedUri $hiddenDataPath")
                }
            }
        )
    }


    /**
     * Add hidden data to image
     */
    fun addHiddenData(filePath: String, context: Context): String {
        val aesKey = AESHelper.generateKey()
        val data = JSONObject()
        Log.d(TAG, "AES key: ${AESHelper.bytesToHex(aesKey)}")

        // Get location
        val location = locationService?.getCurrentLocation()

        if (location != null) {
            data.put("latitude", location.latitude)
            data.put("longitude", location.longitude)

            Log.d(TAG, "Location: ${location.latitude}, ${location.longitude}")
        } else {
            Toast.makeText(this, "Location is null", Toast.LENGTH_SHORT).show()
        }

        val dataString = data.toString()
        Log.d(TAG, "Data: $dataString")
        val dataBytes = dataString.toByteArray()

        val encData = "${AESHelper.bytesToHex(aesKey)}${
            AESHelper.bytesToHex(
                AESHelper.encrypt(
                    dataBytes,
                    aesKey
                )
            )
        }"

        val newFileName = filePath.split("/").last().split(".")[0] + "_watermark.jpg"
        val newFilePath = filePath.split("/").dropLast(1).joinToString("/") + "/" + newFileName

        // Get image buffer and add FFT watermark
        val bitmap: Bitmap = BitmapFactory.decodeFile(filePath)
        val mutableBitmap = bitmap.copy(bitmap.config, true)
        var watermarkedBitmap = mutableBitmap

        try {
            if (location != null) {
                watermarkedBitmap = FFTWatermarkHelper.doAddWatermark(
                    mutableBitmap,
                    "${location.latitude}${location.longitude}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Watermark failed: ${e.message}", e)
        }

        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                newFileName
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PictureDataHider")
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val os = resolver.openOutputStream(uri!!)
        if (os != null) {
            watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
        }
        os!!.close()


        // Add exif data
        try {
            val exifInterface = ExifInterface(filePath)
            val exifInterface2 = ExifInterface(newFilePath)

            // Get all EXIF data and copy to new file
            val attributes = arrayOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_IMAGE_LENGTH,
                ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.TAG_SUBSEC_TIME,
                ExifInterface.TAG_WHITE_BALANCE,
            )

            for (attribute in attributes) {
                exifInterface2.setAttribute(attribute, exifInterface.getAttribute(attribute))
            }

            exifInterface2.setAttribute(
                ExifInterface.TAG_USER_COMMENT,
                encData
            )
            exifInterface2.saveAttributes()
            Log.d(TAG, "Exif data saved.")
        } catch (e: Exception) {
            Log.e(TAG, "Exif data save failed: ${e.message}", e)
        }


        // Delete original file
        val file = java.io.File(filePath)
        file.delete()

        Log.d(TAG, "LSB data saved.")

        return newFilePath
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val FILENAME_FORMAT = "yyyyMMddHHmmssSSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}