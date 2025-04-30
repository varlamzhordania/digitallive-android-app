package com.digitallive.leddisplay.utils

import com.digitallive.leddisplay.models.Ticker
import com.digitallive.leddisplay.models.TickerItem
import com.digitallive.leddisplay.models.VideoData
import org.json.JSONObject

object JsonUtils {

    fun extractVideoDataFromJson(jsonData: JSONObject): VideoData {
        val tickersList = mutableListOf<Ticker>()
        val tickersArray = jsonData.optJSONArray("tickers")

        if (tickersArray != null) {
            for (i in 0 until tickersArray.length()) {
                val tickerObj = tickersArray.getJSONObject(i)
                val itemsArray = tickerObj.optJSONArray("items")
                val tickerItems = mutableListOf<TickerItem>()

                if (itemsArray != null) {
                    for (j in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(j)
                        tickerItems.add(
                            TickerItem(
                                id = itemObj.optInt("id"),
                                isActive = itemObj.optBoolean("is_active"),
                                createdAt = itemObj.optString("created_at"),
                                updatedAt = itemObj.optString("updated_at"),
                                content = itemObj.optString("content"),
                                order = itemObj.optInt("order"),
                                ticker = itemObj.optInt("ticker")
                            )
                        )
                    }
                }

                tickersList.add(
                    Ticker(
                        id = tickerObj.optInt("id"),
                        interval = tickerObj.optDouble("interval")
                            .let { if (it.isNaN()) null else it },
                        startTime = tickerObj.optString("start_time"),
                        endTime = tickerObj.optString("end_time"),
                        items = tickerItems
                    )
                )
            }
        }

        val videoData = VideoData(
            isActive = jsonData.optBoolean("is_active", true),
            updatedAt = jsonData.optString("updated_at", System.currentTimeMillis().toString()),
            name = jsonData.optString("name", ""),
            slug = jsonData.optString("slug", ""),
            currentVideo = jsonData.optString("current_video").let {
                if (it.contains("127.0.0.1")) it.replace("127.0.0.1", "192.168.0.176")
                else it
            },
            videoDuration = if (jsonData.isNull("video_duration")) null else jsonData.optLong("video_duration"),
            zoomToFill = jsonData.optBoolean("zoom_to_fill", false),
            loop = jsonData.optInt("loop", -1),
            paused = jsonData.optBoolean("paused", false),
            tickers = tickersList
        )

        return videoData
    }
}