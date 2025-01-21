package com.tangem.datasource.api.visa

import com.tangem.datasource.api.visa.models.response.CardActivationRemoteStateResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface TangemVisaApi {

    @GET("activation-status")
    suspend fun getRemoteActivationStatus(
        @Header("Authorization") authHeader: String,
    ): CardActivationRemoteStateResponse
}