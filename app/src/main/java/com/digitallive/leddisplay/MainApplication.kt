package com.digitallive.leddisplay

import android.app.Application
import android.content.Context
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import java.lang.ref.WeakReference

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        contextReference = WeakReference(applicationContext)

        val config: PRDownloaderConfig = PRDownloaderConfig.newBuilder()
            .setReadTimeout(30000)
            .setConnectTimeout(30000)
            .setDatabaseEnabled(false)
            .build()
        PRDownloader.initialize(applicationContext, config)
    }

    companion object {
        private lateinit var instance: MainApplication
        private lateinit var contextReference: WeakReference<Context>

        val appContext: Context
            get() {
                if (!this::contextReference.isInitialized || contextReference.get() == null) {
                    contextReference = WeakReference(
                        getInstance().applicationContext
                    )
                }
                return contextReference.get()!!
            }

        private fun getInstance(): MainApplication {
            if (!this::instance.isInitialized) {
                instance = MainApplication()
            }
            return instance
        }
    }
}