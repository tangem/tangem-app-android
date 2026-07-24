package com.tangem.datasource.api.polymarket.clob

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.polymarket.clob.models.PolymarketApiKeyResponse
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface PolymarketClobApi {

    @POST("auth/api-key")
    suspend fun createApiKey(@HeaderMap headers: Map<String, String>): ApiResponse<PolymarketApiKeyResponse>

    @GET("auth/derive-api-key")
    suspend fun deriveApiKey(@HeaderMap headers: Map<String, String>): ApiResponse<PolymarketApiKeyResponse>
}