package com.tangem.grow.datasource.polymarket

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketApprovalCallDto
import com.tangem.grow.datasource.polymarket.models.PolymarketCategoriesResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketConfigurationResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketEventResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketEventsResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketOrderQuoteRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketOrderQuoteResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketSearchResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketPositionsResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketClosedPositionsResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketPositionResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletApprovalsRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletOperationResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletStatusResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Polymarket predictions BFF Discovery API (`api/predictions/v1`).
 *
 * Provided in [com.tangem.grow.datasource.di.GrowDatasourceModule].
 */
interface PolymarketApi {

    @GET("api/predictions/v1/positions?sort=endDate&ascending=true")
    suspend fun getPositions(
        @Header("wallet-id") walletId: String,
        @Query("depositWalletAddress") depositWalletAddress: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): ApiResponse<PolymarketPositionsResponse>

    @GET("api/predictions/v1/positions/closed?sort=closedAt&ascending=false")
    suspend fun getClosedPositions(
        @Header("wallet-id") walletId: String,
        @Query("depositWalletAddress") depositWalletAddress: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): ApiResponse<PolymarketClosedPositionsResponse>

    @GET("api/predictions/v1/positions/outcome")
    suspend fun getPosition(
        @Header("wallet-id") walletId: String,
        @Query("depositWalletAddress") depositWalletAddress: String,
        @Query("conditionId") conditionId: String,
        @Query("assetId") assetId: String,
    ): ApiResponse<PolymarketPositionResponse>

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
     * Full-text search over discoverable prediction events. Pages with a 1-based [page] number,
     * unlike the cursor-driven feed.
     *
     * @param query search text, required by the BFF
     * @param limit page size (BFF default 20)
     * @param page 1-based page number (BFF default 1)
     */
    @GET("api/predictions/v1/search")
    suspend fun searchEvents(
        @Query("query") query: String,
        @Query("limit") limit: Int,
        @Query("page") page: Int,
    ): ApiResponse<PolymarketSearchResponse>

    /**
     * Details of a single prediction event. Unlike the feed, which carries only the top active
     * markets of an event, this endpoint carries all of them (including closed ones).
     */
    @GET("api/predictions/v1/events/{eventId}")
    suspend fun getEvent(@Path("eventId") eventId: String): ApiResponse<PolymarketEventResponse>

    /**
     * Onboarding status of the owner's deposit wallet — the endpoint the client polls to observe
     * deploy/approval progress. Serves the stored status; no on-chain calls on the read path.
     */
    @GET("api/predictions/v1/wallet")
    suspend fun getWalletStatus(
        @Query("ownerAddress") ownerAddress: String,
    ): ApiResponse<PolymarketWalletStatusResponse>

    /**
     * The same status, looked up by the Tangem wallet id the deposit wallet was bound to on first deploy.
     * This is the only way to ask about a wallet whose owner address this device has never derived.
     */
    @GET("api/predictions/v1/wallet")
    suspend fun getWalletStatusByWalletId(
        @Query("walletId") walletId: String,
    ): ApiResponse<PolymarketWalletStatusResponse>

    /** @param walletId Tangem wallet id, 64 hex without the `0x` prefix. */
    @GET("api/predictions/v1/configuration")
    suspend fun getConfiguration(@Header("wallet-id") walletId: String): ApiResponse<PolymarketConfigurationResponse>

    /**
     * Initiate deposit-wallet deployment via the relayer (gasless, unsigned). Returns as soon as the
     * relayer accepts the submission; the client then polls [getWalletStatus].
     */
    @POST("api/predictions/v1/wallet/deploy")
    suspend fun deployWallet(
        @Body request: PolymarketWalletDeployRequest,
    ): ApiResponse<PolymarketWalletOperationResponse>

    /**
     * Relay the fully-signed 13-approval batch (gasless). The DW must be deployed first. Returns as soon
     * as the relayer accepts; the client then polls [getWalletStatus].
     */
    @POST("api/predictions/v1/wallet/approvals")
    suspend fun submitApprovals(
        @Body request: PolymarketWalletApprovalsRequest,
    ): ApiResponse<PolymarketWalletOperationResponse>

    /**
     * Preview the execution of a fill-and-kill order against the live order book. Read-only: nothing is
     * placed, and the quote is only valid for as long as the book stands still, so callers re-request it.
     */
    @POST("api/predictions/v1/orders/quote")
    suspend fun getOrderQuote(@Body request: PolymarketOrderQuoteRequest): ApiResponse<PolymarketOrderQuoteResponse>

    /**
     * Relay the signed pUSD transfer that empties part of the deposit wallet (gasless). Always `202` on
     * acceptance; resubmitting the identical batch answers with the same `transactionId`.
     */
    @POST("api/predictions/v1/withdraw")
    suspend fun submitWithdraw(
        @Header("wallet-id") walletId: String,
        @Body request: PolymarketRelayedBatchRequest,
    ): ApiResponse<PolymarketRelayedBatchResponse>

    /**
     * Read leg 1 from the BFF's own record of the withdrawal, not from the relayer. A `404` means the id is
     * not this wallet's withdrawal.
     */
    @GET("api/predictions/v1/withdraw/{transactionId}")
    suspend fun getWithdrawState(
        @Header("wallet-id") walletId: String,
        @Path("transactionId") transactionId: String,
    ): ApiResponse<PolymarketRelayedBatchResponse>

    /**
     * Read the single contract call that redeems a resolved position. Offline lookup: it relays nothing and
     * may be retried freely.
     */
    @GET("api/predictions/v1/positions/redeem/call")
    suspend fun getRedeemCall(
        @Header("wallet-id") walletId: String,
        @Query("conditionId") conditionId: String,
        @Query("negRisk") isNegRisk: Boolean,
    ): ApiResponse<PolymarketApprovalCallDto>

    /**
     * Relay the signed redeem call (gasless). Idempotent per (wallet, market): a claim already in flight
     * answers with its own `transactionId` and current state instead of relaying a second one.
     */
    @POST("api/predictions/v1/positions/redeem")
    suspend fun submitRedeem(
        @Header("wallet-id") walletId: String,
        @Body request: PolymarketRelayedBatchRequest,
    ): ApiResponse<PolymarketRelayedBatchResponse>

    /**
     * Read the BFF's own record of the claim. A `404` means the id is not this wallet's claim — stop polling.
     */
    @GET("api/predictions/v1/positions/redeem/{transactionId}")
    suspend fun getRedeemState(
        @Header("wallet-id") walletId: String,
        @Path("transactionId") transactionId: String,
    ): ApiResponse<PolymarketRelayedBatchResponse>
}