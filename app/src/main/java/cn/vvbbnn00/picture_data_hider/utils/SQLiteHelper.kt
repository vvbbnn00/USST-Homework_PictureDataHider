package cn.vvbbnn00.picture_data_hider.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import cn.vvbbnn00.picture_data_hider.models.Photo

class SQLiteHelper(context: Context) {
    private var filePath = ""
    private var db: SQLiteDatabase? = null

    init {
        val filePath = context.filesDir.path
        this.filePath = "$filePath/${SQLITE_FILE}"
        db = SQLiteDatabase.openOrCreateDatabase(this.filePath, null)

        init()
    }

    /**
     * Init Database
     */
    private fun init() {
        db!!.execSQL(
            "CREATE TABLE IF NOT EXISTS photos (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "path TEXT," +
                    "timestamp INTEGER," +
                    "latitude REAL," +
                    "longitude REAL," +
                    "orientation INTEGER" +
                    ")"
        )
    }


    /**
     * Insert a photo into database
     */
    fun insert(photo: Photo) {
        val contentValues = ContentValues()
        contentValues.put("path", photo.path)
        contentValues.put("timestamp", photo.timestamp)
        contentValues.put("latitude", photo.latitude)
        contentValues.put("longitude", photo.longitude)
        contentValues.put("orientation", photo.orientation)
        db!!.insert("photos", null, contentValues)
    }


    /**
     * Query a photo by id
     */
    @SuppressLint("Range")
    fun query(id: Int): Photo? {
        val cursor = db!!.rawQuery("SELECT * FROM photos WHERE id = ?", arrayOf(id.toString()))
        if (cursor.moveToNext()) {
            val photo = Photo(
                cursor.getInt(cursor.getColumnIndex("id")),
                cursor.getString(cursor.getColumnIndex("path")),
                cursor.getLong(cursor.getColumnIndex("timestamp")),
                cursor.getDouble(cursor.getColumnIndex("latitude")),
                cursor.getDouble(cursor.getColumnIndex("longitude")),
                cursor.getInt(cursor.getColumnIndex("orientation"))
            )
            cursor.close()
            return photo
        }
        cursor.close()
        return null
    }

    /**
     * Query All Photos
     */
    @SuppressLint("Range")
    fun queryAll(type: String = "all"): List<Photo> {
        val querySQL = when (type) {
            "recentWeek" -> "SELECT * FROM photos WHERE timestamp > strftime('%s', 'now', '-7 day') ORDER BY timestamp DESC"
            "recentMonth" -> "SELECT * FROM photos WHERE timestamp > strftime('%s', 'now', '-30 day') ORDER BY timestamp DESC"
            else -> "SELECT * FROM photos ORDER BY timestamp DESC"
        }

        val cursor = db!!.rawQuery(querySQL, null)
        val photos = mutableListOf<Photo>()
        while (cursor.moveToNext()) {
            val photo = Photo(
                cursor.getInt(cursor.getColumnIndex("id")),
                cursor.getString(cursor.getColumnIndex("path")),
                cursor.getLong(cursor.getColumnIndex("timestamp")),
                cursor.getDouble(cursor.getColumnIndex("latitude")),
                cursor.getDouble(cursor.getColumnIndex("longitude")),
                cursor.getInt(cursor.getColumnIndex("orientation"))
            )
            photos.add(photo)
        }
        cursor.close()
        return photos
    }

    /**
     * Delete a photo by id from sqlite (mention that the photo file will not be deleted)
     */
    fun delete(id: Int) {
        db!!.delete("photos", "id = ?", arrayOf(id.toString()))
    }

    companion object {
        private const val SQLITE_FILE = "data.db"
    }


}