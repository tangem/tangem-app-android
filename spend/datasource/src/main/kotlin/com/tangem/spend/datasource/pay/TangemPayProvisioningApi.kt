package com.tangem.spend.datasource.pay

import com.tangem.core.remote.response.ApiResponse
import com.tangem.spend.datasource.pay.models.response.CardProcessorDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface TangemPayProvisioningApi {

    @GET("v1/customer/card/{card_id}/processor/details")
    suspend fun getCardProcessorDetails(
        @Header("Authorization") authHeader: String,
        @Path("card_id") cardId: String,
    ): ApiResponse<CardProcessorDetailsResponse>
}