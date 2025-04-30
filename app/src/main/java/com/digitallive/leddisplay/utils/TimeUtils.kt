package com.digitallive.leddisplay.utils

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {

    fun isWithinTimeRange(start: String?, end: String?): Boolean {
        if (start.isNullOrEmpty() || end.isNullOrEmpty() ||
            start == "null" || end == "null"
        ) return true

        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val now = OffsetDateTime.now()

        return try {
            val startTime = OffsetDateTime.parse(start, formatter)
            val endTime = OffsetDateTime.parse(end, formatter)
            now.isAfter(startTime) && now.isBefore(endTime)
        } catch (_: Exception) {
            false
        }
    }
}