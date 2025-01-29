package com.tangem.lib.visa.api

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.lib.visa.model.VisaTxHistoryResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface VisaApi {

    @GET("transaction")
    suspend fun getTxHistory(
        @Header("Authorization") authorizationHeader: String,
        @Query("card_public_key") cardPublicKey: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): ApiResponse<VisaTxHistoryResponse>
}