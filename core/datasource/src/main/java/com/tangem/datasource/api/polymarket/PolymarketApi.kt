package com.tangem.datasource.api.polymarket

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.polymarket.models.PolymarketCategoriesResponse
import com.tangem.datasource.api.polymarket.models.PolymarketEventsResponse
import com.tangem.datasource.api.polymarket.models.PolymarketWalletApprovalsRequest
import com.tangem.datasource.api.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.datasource.api.polymarket.models.PolymarketWalletOperationResponse
import com.tangem.datasource.api.polymarket.models.PolymarketWalletStatusResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Polymarket predictions BFF Discovery API (`api/predictions/v1`).
 *
 * Provided in [com.tangem.datasource.di.NetworkModule] via the [ApiConfig.ID.Predictions] config.
 */
interface PolymarketApi {

    /**
     * BFF-owned UI categories shown as Discovery feed tabs.
     *
     * @param locale optional locale for the category labels; BFF defaults to `en`
     */
    @GET("api/predictions/v1/categories")
    suspend fun getCategories(@Query("locale") locale: String?): ApiResponse<PolymarketCategoriesResponse>

    /**
     * Discovery feed: paginated prediction events, each with its top active markets.
     *
     * @param category optional category id to filter by; `null` for the default (Trending) feed
     * @param limit page size (BFF default 20)
     * @param cursor keyset pagination cursor; `null` for the first page
     */
    @GET("api/predictions/v1/events")
    suspend fun getEvents(
        @Query("category") category: Int?,
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): ApiResponse<PolymarketEventsResponse>

    /**
     * Onboarding status of the owner's deposit wallet — the endpoint the client polls to observe
     * deploy/approval progress. Serves the stored status; no on-chain calls on the read path.
     */
    @GET("api/predictions/v1/wallet")
    suspend fun getWalletStatus(
        @Query("ownerAddress") ownerAddress: String,
    ): ApiResponse<PolymarketWalletStatusResponse>

    /**
     * Initiate deposit-wallet deployment via the relayer (gasless, unsigned). Returns as soon as the
     * relayer accepts the submission; the client then polls [getWalletStatus].
     */
    @POST("api/predictions/v1/wallet/deploy")
    suspend fun deployWallet(
        @Body request: PolymarketWalletDeployRequest,
    ): ApiResponse<PolymarketWalletOperationResponse>

    /**
     * Relay the fully-signed 6-approval batch (gasless). The DW must be deployed first. Returns as soon
     * as the relayer accepts; the client then polls [getWalletStatus].
     */
    @POST("api/predictions/v1/wallet/approvals")
    suspend fun submitApprovals(
        @Body request: PolymarketWalletApprovalsRequest,
    ): ApiResponse<PolymarketWalletOperationResponse>
}