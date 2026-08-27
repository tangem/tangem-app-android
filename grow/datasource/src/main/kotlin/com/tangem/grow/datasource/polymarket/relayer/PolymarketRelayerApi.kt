package com.tangem.grow.datasource.polymarket.relayer

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.polymarket.relayer.models.PolymarketNonceResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PolymarketRelayerApi {

    @GET("nonce")
    suspend fun getNonce(
        @Query("address") address: String,
        @Query("type") type: String,
    ): ApiResponse<PolymarketNonceResponse>
}