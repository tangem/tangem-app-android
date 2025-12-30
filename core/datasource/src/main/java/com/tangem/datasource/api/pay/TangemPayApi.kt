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
        @Header("X-API-KEY") authHeader: String,
        @Path("customer_wallet_id") customerWalletId: String,
    ): ApiResponse<CheckCustomerWalletResponse>

    @PATCH("v1/customer/pay-enabled")
    suspend fun setTangemPayEnabledStatus(
        @Header("Authorization") authHeader: String,
        @Body body: SetTangemPayEnabledRequest,
    ): ApiResponse<Any>

    @POST("v1/deeplink/validate")
    suspend fun validateDeeplink(@Body body: DeeplinkValidityRequest): ApiResponse<DeeplinkValidityResponse>

    @GET("v1/customer/eligibility")
    suspend fun checkCustomerEligibility(): ApiResponse<CustomerEligibilityResponse>

    @GET("v1/order/{order_id}")
    suspend fun getOrder(
        @Header("Authorization") authHeader: String,
        @Path("order_id") orderId: String,
    ): ApiResponse<OrderResponse>

    @POST("v1/order")
    suspend fun createOrder(
        @Header("Authorization") authHeader: String,
        @Body body: OrderRequest,
    ): ApiResponse<OrderResponse>

    @GET("v1/customer/balance")
    suspend fun getCardBalance(@Header("Authorization") authHeader: String): ApiResponse<CardBalanceResponse>

    @POST("v1/customer/card/details")
    suspend fun revealCardDetails(
        @Header("Authorization") authHeader: String,
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
}