package com.tangem.datasource.api.pay

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.pay.models.request.*
import com.tangem.datasource.api.pay.models.response.*
import retrofit2.http.*

private const val TX_HISTORY_PAGING_DEFAULT_LIMIT = 20

@Suppress("TooManyFunctions")
interface TangemPayApi {

    @GET("v1/customer/transactions")
    suspend fun getTangemPayTxHistory(
        @Header("Authorization") authHeader: String,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int = TX_HISTORY_PAGING_DEFAULT_LIMIT,
    ): ApiResponse<TangemPayTxHistoryResponse>

    @GET("v1/customer/kyc")
    suspend fun getKycAccess(@Header("Authorization") authHeader: String): ApiResponse<KycAccessInfoResponse>

    @GET("v1/customer/me")
    suspend fun getCustomerMe(@Header("Authorization") authHeader: String): ApiResponse<CustomerMeResponse>

    @GET("v1/customer/wallets/{customer_wallet_id}")
    suspend fun checkCustomerWalletId(
        @Path("customer_wallet_id") customerWalletId: String,
    ): ApiResponse<CheckCustomerWalletResponse>

    @PATCH("v1/customer/pay-enabled")
    suspend fun setTangemPayEnabledStatus(
        @Header("Authorization") authHeader: String,
        @Body body: SetTangemPayEnabledRequest,
    ): ApiResponse<Any>

    @POST("v1/deeplink/validate")
    suspend fun validateDeeplink(@Body body: DeeplinkValidityRequest): ApiResponse<DeeplinkValidityResponse>

    @GET("v1/eligibility/channels")
    suspend fun getEligibilityChannels(): ApiResponse<TangemPayEligibilityChannels>

    @GET("v1/order/{order_id}")
    suspend fun getOrder(
        @Header("Authorization") authHeader: String,
        @Path("order_id") orderId: String,
    ): ApiResponse<OrderResponse>

    /**
     * Find user orders, filtered by types and/or statuses. Source of truth for resolving active orders.
     *
     * Multiple values for the same query key are sent as repeated `order_types=A&order_types=B` params.
     */
    @GET("v1/order")
    suspend fun findOrders(
        @Header("Authorization") authHeader: String,
        @Query("order_types") orderTypes: List<String>?,
        @Query("order_statuses") orderStatuses: List<String>?,
    ): ApiResponse<FindOrdersResponse>

    @POST("v1/order")
    suspend fun createOrder(
        @Header("Authorization") authHeader: String,
        @Body body: OrderRequest,
    ): ApiResponse<OrderResponse>

    /** Customer offers — used to gate the issue-additional-card flow. */
    @GET("v1/customer/offers")
    suspend fun getCustomerOffers(@Header("Authorization") authHeader: String): ApiResponse<CustomerOffersResponse>

    @GET("v1/customer/balance")
    suspend fun getCardBalance(@Header("Authorization") authHeader: String): ApiResponse<CardBalanceResponse>

    /** Card-scoped reveal. `{card_id}` = selected card id. */
    @POST("v1/customer/card/{card_id}/details")
    suspend fun revealCardDetails(
        @Header("Authorization") authHeader: String,
        @Path("card_id") cardId: String,
        @Body body: CardDetailsRequest,
    ): ApiResponse<CardDetailsResponse>

    @GET("v1/customer/card/pin")
    suspend fun getPin(
        @Header("Authorization") authHeader: String,
        @Header("X-Session-Id") sessionId: String,
    ): ApiResponse<GetPinResponse>

    @PUT("v1/customer/card/pin")
    suspend fun setPin(
        @Header("Authorization") authHeader: String,
        @Body body: SetPinRequest,
    ): ApiResponse<SetPinResponse>

    @POST("v1/customer/card/freeze")
    suspend fun freezeCard(
        @Header("Authorization") authHeader: String,
        @Body body: FreezeUnfreezeCardRequest,
    ): ApiResponse<FreezeUnfreezeCardResponse>

    @POST("v1/customer/card/unfreeze")
    suspend fun unfreezeCard(
        @Header("Authorization") authHeader: String,
        @Body body: FreezeUnfreezeCardRequest,
    ): ApiResponse<FreezeUnfreezeCardResponse>

    @GET("v1/fees/{type}")
    suspend fun getFee(
        @Header("Authorization") authHeader: String,
        @Path("type") type: String,
    ): ApiResponse<FeeResponse>

    @POST("v1/customer/card/reissue")
    suspend fun reissueCard(
        @Header("Authorization") authHeader: String,
        @Body body: ReissueCardRequest,
    ): ApiResponse<ReissueCardResponse>

    @POST("v1/customer/card/close")
    suspend fun closeCard(
        @Header("Authorization") authHeader: String,
        @Body body: CloseCardRequest,
    ): ApiResponse<CloseCardResponse>

    @POST("v1/customer/card/withdraw/data")
    suspend fun getWithdrawData(
        @Header("Authorization") authHeader: String,
        @Body body: WithdrawDataRequest,
    ): ApiResponse<WithdrawDataResponse>

    @POST("v1/customer/card/withdraw")
    suspend fun withdraw(
        @Header("Authorization") authHeader: String,
        @Body body: WithdrawRequest,
    ): ApiResponse<WithdrawResponse>

    @PATCH("v1/customer/card/{card_id}")
    suspend fun updateCard(
        @Header("Authorization") authHeader: String,
        @Body body: UpdateCardRequest,
        @Path("card_id") cardId: String,
    ): ApiResponse<UpdateCardDisplayNameResponse>
}