package com.digitallive.leddisplay.utils

import android.content.Context
import java.io.File

object FileUtils {

    fun getUniqueFileName(baseDir: File, originalName: String): String {
        var file = File(baseDir, originalName)
        if (!file.exists()) return originalName

        val nameWithoutExt = originalName.substringBeforeLast(".")
        val ext = originalName.substringAfterLast(".", "")
        var index = 1

        while (file.exists()) {
            val newName = if (ext.isNotEmpty()) {
                "${nameWithoutExt}_$index.$ext"
            } else {
                "${nameWithoutExt}_$index"
            }
            file = File(baseDir, newName)
            index++
        }

        return file.name
    }

    fun deleteOtherVideoFiles(context: Context, currentFileName: String) {
        val videoExtensions =
            listOf("mp4", "mkv", "avi", "webm", "mov", "flv", "wmv", "mpeg", "temp")

        context.filesDir.listFiles()?.forEach { file ->
            val ext = file.extension.lowercase()
            if (file.name != currentFileName && ext in videoExtensions) {
                file.delete()
            }
        }
    }
}