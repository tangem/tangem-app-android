package com.tangem.datasource.api.visa

import com.tangem.datasource.api.visa.models.response.CardActivationRemoteStateResponse
import retrofit2.http.GET

interface TangemVisaApi {

    @GET("activation-status")
    suspend fun getRemoteActivationStatus(): CardActivationRemoteStateResponse
}