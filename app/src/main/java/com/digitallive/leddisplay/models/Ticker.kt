package com.digitallive.leddisplay.models

import com.google.gson.annotations.SerializedName

data class Ticker(
    val id: Int,
    val interval: Double?,
    @SerializedName("start_time") val startTime: String?,
    @SerializedName("end_time") val endTime: String?,
    val items: List<TickerItem>
)