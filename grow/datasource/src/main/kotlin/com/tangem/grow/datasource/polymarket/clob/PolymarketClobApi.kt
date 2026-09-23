package com.tangem.grow.datasource.polymarket.clob

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketApiKeyResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketBalanceAllowanceResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketOrderResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPlaceOrderResponse
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPriceResponse
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path
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

    /**
     * Reads the CLOB's cached collateral balance and allowance of the authenticated deposit wallet.
     * L2-authenticated; the sibling of [updateBalanceAllowance] that returns the numbers instead of
     * only refreshing them.
     */
    @POST("order")
    suspend fun placeOrder(
        @HeaderMap headers: Map<String, String>,
        @Body body: RequestBody,
    ): ApiResponse<PolymarketPlaceOrderResponse>

    /**
     * Best price on one [side] of the asset's book. Public market data — the only CLOB call that needs
     * no auth headers. A 404 means no orderbook exists for the token, i.e. the market is closed.
     */
    @GET("price")
    suspend fun getPrice(
        @Query("token_id") tokenId: String,
        @Query("side") side: String,
    ): ApiResponse<PolymarketPriceResponse>

    /**
     * One placed order as the book sees it. L2-authenticated, and the signed path carries the id. Polled after
     * `POST /order` answers `delayed`: the order is accepted first and matched later, and this is where the fill
     * shows up.
     */
    @GET("data/order/{id}")
    suspend fun getOrder(
        @HeaderMap headers: Map<String, String>,
        @Path("id") orderId: String,
    ): ApiResponse<PolymarketOrderResponse>

    @GET("balance-allowance")
    suspend fun getBalanceAllowance(
        @HeaderMap headers: Map<String, String>,
        @Query("asset_type") assetType: String,
        @Query("signature_type") signatureType: Int,
    ): ApiResponse<PolymarketBalanceAllowanceResponse>
}