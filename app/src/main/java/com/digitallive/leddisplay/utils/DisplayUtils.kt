package com.digitallive.leddisplay.utils

import android.util.TypedValue
import com.digitallive.leddisplay.MainApplication.Companion.appContext

object DisplayUtils {

    fun Int.toPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            appContext.resources.displayMetrics
        ).toInt()
    }
}