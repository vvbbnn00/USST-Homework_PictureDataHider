package cn.vvbbnn00.picture_data_hider

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import cn.vvbbnn00.picture_data_hider.utils.AESHelper
import cn.vvbbnn00.picture_data_hider.utils.FFTWatermarkHelper
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import java.io.IOException
import java.io.InputStream


class PhotoDecryptionActivity : AppCompatActivity() {

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i("PhotoDecryptionActivity", "OpenCV loaded successfully")
                }

                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(
                "PhotoDecryptionActivity",
                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            )
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d("PhotoDecryptionActivity", "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_decryption)

        findViewById<Button>(R.id.btn_choose_photo).setOnClickListener {
            // 从相册中选择图片
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/jpeg"
            takePictureActivity.launch(intent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun doDecryptionEXIF(inputStream: InputStream) {

        val exifInterface = ExifInterface(inputStream)
        val exif = exifInterface.getAttribute(ExifInterface.TAG_USER_COMMENT)
        if (exif != null) {
            try {
                val key = AESHelper.hexToBytes(exif.substring(0, 32))
                val encryptedData = AESHelper.hexToBytes(exif.substring(32))

                Log.d("PhotoDecryptionActivity", "Key: ${AESHelper.bytesToHex(key)}")
                Log.d(
                    "PhotoDecryptionActivity",
                    "Encrypted data: ${AESHelper.bytesToHex(encryptedData)}"
                )

                val decryptedData = AESHelper.decrypt(encryptedData, key)

                val decryptedStr = String(decryptedData)
                findViewById<TextView>(R.id.txt_result).text = decryptedStr
            } catch (e: Exception) {
                e.printStackTrace()
                findViewById<TextView>(R.id.txt_result).text = "Failed to decrypt data"
            }
        }

    }


    /**
     * Register for activity result
     */
    private var takePictureActivity = registerForActivityResult(
        StartActivityForResult()
    ) {
        if (it.resultCode != RESULT_OK) {
            return@registerForActivityResult
        }
        if (it.data == null) {
            Log.d("PhotoDecryptionActivity", "No data returned")
            return@registerForActivityResult
        }

        val selectedImageUri = it.data!!.data
        if (selectedImageUri == null) {
            Log.d("PhotoDecryptionActivity", "No image selected")
            return@registerForActivityResult
        }

        try {
            // 使用ContentResolver获取Bitmap
            val resolver = contentResolver
            var inputStream = resolver.openInputStream(selectedImageUri)
            if (inputStream != null) {
                doDecryptionEXIF(inputStream)
            } else {
                Log.e("PhotoDecryptionActivity", "Failed to open input stream")
            }

            // 重置输入流
            inputStream?.close()
            inputStream = resolver.openInputStream(selectedImageUri)

            val bitmap = BitmapFactory.decodeStream(inputStream)
            var decryptedBitmap = bitmap

            try {
                decryptedBitmap = FFTWatermarkHelper.doGetWatermark(bitmap)

                // 保存解密后的图片
                val outName =
                    "decrypted_watermark_" + System.currentTimeMillis() + ".jpg"

                val contentValues = ContentValues().apply {
                    put(
                        MediaStore.MediaColumns.DISPLAY_NAME,
                        outName
                    )
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                        put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "Pictures/PictureDataHider"
                        )
                    }
                }

                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                val outputStream = contentResolver.openOutputStream(uri!!)
                if (outputStream != null) {
                    decryptedBitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        100,
                        outputStream
                    )
                }

                outputStream?.close()

                Toast.makeText(
                    this,
                    "Decrypted watermark saved into $outName",
                    Toast.LENGTH_SHORT
                )
                    .show()

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Failed to decrypt watermark",
                    Toast.LENGTH_SHORT
                )
                    .show()
                Log.e(
                    "PhotoDecryptionActivity",
                    "Failed to decrypt watermark, " + e.message
                )
            }

            findViewById<ImageView>(R.id.img_result).setImageBitmap(decryptedBitmap)

            // Close input stream
            inputStream?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}