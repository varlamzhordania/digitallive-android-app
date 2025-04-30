package com.digitallive.leddisplay.utils

import android.view.View

object ViewUtils {

    fun View.detectEightQuickTaps(onEightTaps: () -> Unit) {
        var tapCount = 0
        var firstTapTime = 0L

        this.setOnClickListener {
            val currentTime = System.currentTimeMillis()

            if (tapCount == 0) {
                firstTapTime = currentTime
            }

            tapCount++

            if (currentTime - firstTapTime > 2000L) {
                // too late — reset
                tapCount = 1
                firstTapTime = currentTime
            }

            if (tapCount == 8) {
                tapCount = 0
                firstTapTime = 0L
                onEightTaps()
            }
        }
    }
}