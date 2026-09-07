package com.tangem.grow.datasource.polymarket.bridge

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketBridgeAddressesDto
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketDepositRequest
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketSupportedAssetsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface PolymarketBridgeApi {

    @GET("supported-assets")
    suspend fun getSupportedAssets(): ApiResponse<PolymarketSupportedAssetsResponse>

    /** @param builderCode required: without it the call succeeds, only warns, and loses attribution. */
    @POST("deposit")
    suspend fun createDepositAddresses(
        @Header("X-Builder-Code") builderCode: String,
        @Body body: PolymarketDepositRequest,
    ): ApiResponse<PolymarketBridgeAddressesDto>
}