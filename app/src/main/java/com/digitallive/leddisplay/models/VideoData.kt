package com.digitallive.leddisplay.models

import com.google.gson.annotations.SerializedName

data class VideoData(
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("updated_at") val updatedAt: String,
    val name: String,
    val slug: String,
    @SerializedName("current_video") val currentVideo: String,
    @SerializedName("video_duration") val videoDuration: Long?,
    @SerializedName("zoom_to_fill") val zoomToFill: Boolean = false,
    val loop: Int,
    val paused: Boolean,
    val tickers: List<Ticker>
)