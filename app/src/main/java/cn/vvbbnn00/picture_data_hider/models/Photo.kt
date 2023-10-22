package cn.vvbbnn00.picture_data_hider.models

data class Photo(
    val id: Int,
    val path: String,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val orientation: Int
)
