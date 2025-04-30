package com.digitallive.leddisplay.models

import com.google.gson.annotations.SerializedName

data class TickerItem(
    val id: Int,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val content: String,
    val order: Int,
    val ticker: Int
)