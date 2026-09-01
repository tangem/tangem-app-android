package com.tangem.grow.datasource.polymarket.bridge

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketBridgeAddressesDto
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketDepositRequest
import com.tangem.grow.datasource.polymarket.bridge.models.PolymarketSupportedAssetsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Polymarket bridge (`https://bridge.polymarket.com`).
 *
 * Polymarket's own service, not the Tangem BFF: it takes neither `api-key` nor `wallet-id`. Funding a
 * deposit wallet goes through it exclusively — a transfer sent straight to the deposit wallet is not
 * converted to the trading collateral and never reaches the balance.
 *
 * Provided in [com.tangem.grow.datasource.di.GrowDatasourceModule].
 */
interface PolymarketBridgeApi {

    /**
     * @return the chains and tokens accepted right now, with their per-asset deposit minimums. Read before
     *  every deposit: the list changes, and offering a combination absent from it can lose the funds.
     */
    @GET("supported-assets")
    suspend fun getSupportedAssets(): ApiResponse<PolymarketSupportedAssetsResponse>

    /**
     * @param builderCode Tangem's identifier at Polymarket, `0x` + 64 hex. A request without it still
     *  succeeds but returns a `missing_builder_code` warning, so the loss of attribution is silent.
     */
    @POST("deposit")
    suspend fun createDepositAddresses(
        @Header("X-Builder-Code") builderCode: String,
        @Body body: PolymarketDepositRequest,
    ): ApiResponse<PolymarketBridgeAddressesDto>
}