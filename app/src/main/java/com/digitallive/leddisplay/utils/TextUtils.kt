package com.digitallive.leddisplay.utils

import java.util.Locale

object TextUtils {

    fun getProgressDisplayLine(currentBytes: Long, totalBytes: Long): String {
        return String.format(
            "%s / %s",
            getBytesToMBString(currentBytes),
            getBytesToMBString(totalBytes)
        )
    }

    private fun getBytesToMBString(bytes: Long): String {
        return String.format(Locale.ENGLISH, "%.2f MB", bytes / (1024.00 * 1024.00))
    }
}