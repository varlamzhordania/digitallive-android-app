package com.digitallive.leddisplay.api

import com.digitallive.leddisplay.models.ErrorLog
import com.digitallive.leddisplay.utils.DataManager
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface LogApiService {

    @POST("api/displaylog/")
    suspend fun sendLog(
        @Body log: ErrorLog,
        @Header("Authorization") token: String = "Token ${DataManager.getDisplayToken()}"
    ): Response<Unit>
}