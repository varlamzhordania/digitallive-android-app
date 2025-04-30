package com.digitallive.leddisplay.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupEdgeToEdge()
    }

    private fun setupEdgeToEdge() {
        val window = window
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val viewGroup = getWindow().decorView.findViewById<ViewGroup>(android.R.id.content)

            ViewCompat.setOnApplyWindowInsetsListener(viewGroup) { v: View, windowInsets: WindowInsetsCompat ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val params = v.layoutParams as MarginLayoutParams

                v.setPadding(
                    params.leftMargin + insets.left,
                    0,
                    params.rightMargin + insets.right,
                    0
                )

                params.topMargin = 0
                params.bottomMargin = 0

                v.setLayoutParams(params)

                windowInsets
            }
        }
    }

    @Suppress("deprecation")
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}