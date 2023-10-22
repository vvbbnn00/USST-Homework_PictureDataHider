package cn.vvbbnn00.picture_data_hider

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.vvbbnn00.picture_data_hider.models.Photo
import cn.vvbbnn00.picture_data_hider.utils.SQLiteHelper
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.text.SimpleDateFormat

class AlbumActivity : AppCompatActivity() {
    val TAG = "AlbumActivity"
    var photos: List<Photo>? = null
    var queryType: String = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_album)
        findViewById<TabLayout>(R.id.tab_select).setOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                queryType = tab?.text.toString()
                loadPhotos()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Do nothing
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Do nothing
            }
        })

        loadPhotos()
    }


    override fun onResume() {
        super.onResume()
        loadPhotos()
    }

    private fun loadPhotos() {
        photos = SQLiteHelper(this).queryAll(queryType)
        val recyclerView = findViewById<RecyclerView>(R.id.rv_albumn_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AlbumAdapter(photos!!)

        // Toast.makeText(this, "Loaded ${photos!!.size} photos", Toast.LENGTH_SHORT).show()
    }


    class AlbumAdapter(private val photos: List<Photo>) :
        RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivPhoto: ImageView = itemView.findViewById(R.id.iv_photo)
            val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
            val tvDescription: TextView = itemView.findViewById(R.id.tv_description)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
            return ViewHolder(view)
        }

        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val photo = photos[position]

            val absolutePath = photo.path
            // 使用Glide加载图片
            Glide.with(holder.itemView.context)
                .load(File(absolutePath)) // Glide可以直接加载File对象
                .into(holder.ivPhoto)

            holder.tvTitle.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(photo.timestamp)
            holder.tvDescription.text = "Lat: ${photo.latitude}, Lng: ${photo.longitude}"

            holder.itemView.setOnClickListener {
                val photoId = photo.id
                Intent(holder.itemView.context, ShowPhotoActivity::class.java).apply {
                    putExtra("photoId", photoId)
                    holder.itemView.context.startActivity(this)
                }
            }
        }


        override fun getItemCount(): Int = photos.size
    }

}