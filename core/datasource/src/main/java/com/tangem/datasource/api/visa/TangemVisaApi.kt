package com.tangem.datasource.api.visa

import com.tangem.datasource.api.utils.ReadTimeout
import com.tangem.datasource.api.visa.models.response.CardActivationRemoteStateResponse
import retrofit2.http.GET
import retrofit2.http.Header
import java.util.concurrent.TimeUnit

interface TangemVisaApi {

    @GET("activation-status")
    suspend fun getRemoteActivationStatus(
        @Header("Authorization") authHeader: String,
    ): CardActivationRemoteStateResponse

    @ReadTimeout(duration = 20, TimeUnit.MINUTES)
    @GET("activation-status")
    suspend fun getRemoteActivationStatusLongPoll(
        @Header("Authorization") authHeader: String,
    ): CardActivationRemoteStateResponse
}