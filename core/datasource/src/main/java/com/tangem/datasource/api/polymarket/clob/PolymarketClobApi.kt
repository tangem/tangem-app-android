package com.tangem.datasource.api.polymarket.clob

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.polymarket.clob.models.PolymarketApiKeyResponse
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Query

interface PolymarketClobApi {

    @POST("auth/api-key")
    suspend fun createApiKey(@HeaderMap headers: Map<String, String>): ApiResponse<PolymarketApiKeyResponse>

    @GET("auth/derive-api-key")
    suspend fun deriveApiKey(@HeaderMap headers: Map<String, String>): ApiResponse<PolymarketApiKeyResponse>

    /**
     * Refreshes the CLOB's cached collateral balance and allowance for the authenticated deposit wallet.
     * L2-authenticated; the response body is not consumed.
     */
    @GET("balance-allowance/update")
    suspend fun updateBalanceAllowance(
        @HeaderMap headers: Map<String, String>,
        @Query("asset_type") assetType: String,
        @Query("signature_type") signatureType: Int,
    ): ApiResponse<Unit>
}