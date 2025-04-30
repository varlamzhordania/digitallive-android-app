package com.digitallive.leddisplay.api

import com.digitallive.leddisplay.utils.DataManager
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private val BASE_URL by lazy {
        DataManager.getLogServerUrl()!!
    }

    val logApi: LogApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LogApiService::class.java)
    }
}