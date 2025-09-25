package com.tangem.datasource.api.pay

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.pay.models.request.*
import com.tangem.datasource.api.pay.models.response.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

@Suppress("TooManyFunctions")
interface TangemPayApi {

    // region: auth

    @POST("v1/auth/challenge")
    suspend fun generateNonceByCardId(@Body request: GenerateNoneByCardIdRequest): ApiResponse<GenerateNonceResponse>

    @POST("v1/auth/challenge")
    suspend fun generateNonceByCardWallet(
        @Body request: GenerateNoneByCardWalletRequest,
    ): ApiResponse<GenerateNonceResponse>

    @POST("v1/auth/challenge")
    suspend fun generateNonceByCustomerWallet(
        @Body request: GenerateNonceByCustomerWalletRequest,
    ): ApiResponse<GenerateNonceResponse>

    @POST("v1/auth/token")
    suspend fun getAccessTokenByCardId(@Body request: GetAccessTokenByCardIdRequest): ApiResponse<JWTResponse>

    @POST("v1/auth/token")
    suspend fun getTokenByCustomerWallet(@Body request: GetTokenByCustomerWalletRequest): ApiResponse<JWTResponse>

    @POST("v1/auth/token/refresh")
    suspend fun refreshCustomerWalletAccessToken(
        @Body request: RefreshCustomerWalletAccessTokenRequest,
    ): ApiResponse<JWTResponse>

    @POST("v1/auth/token")
    suspend fun getAccessTokenByCardWallet(@Body request: GetAccessTokenByCardWalletRequest): ApiResponse<JWTResponse>

    @POST("v1/auth/token/refresh")
    suspend fun refreshCardIdAccessToken(@Body request: RefreshTokenByCardIdRequest): ApiResponse<JWTResponse>

    @POST("v1/auth/token/refresh")
    suspend fun refreshCardWalletAccessToken(@Body request: RefreshTokenByCardWalletRequest): ApiResponse<JWTResponse>

    @POST("v1/auth/token/exchange")
    suspend fun exchangeAccessToken(@Body request: ExchangeAccessTokenRequest): ApiResponse<JWTResponse>

    // endregion

    // region: activation

    @POST("v1/activation/status")
    suspend fun getRemoteActivationStatus(
        @Header("Authorization") authHeader: String,
        @Body request: ActivationStatusRequest,
    ): ApiResponse<CardActivationRemoteStateResponse>

    @POST("v1/activation/acceptance/message")
    suspend fun getCardWalletAcceptance(
        @Header("Authorization") authHeader: String,
        @Body request: GetCardWalletAcceptanceRequest,
    ): ApiResponse<VisaDataToSignResponse>

    @POST("v1/activation/acceptance/message")
    suspend fun getCustomerWalletAcceptance(
        @Header("Authorization") authHeader: String,
        @Body request: GetCustomerWalletAcceptanceRequest,
    ): ApiResponse<VisaDataToSignResponse>

    @POST("v1/activation/data")
    suspend fun activateByCardWallet(
        @Header("Authorization") authHeader: String,
        @Body body: ActivationByCardWalletRequest,
    ): ApiResponse<Unit>

    @POST("v1/activation/data")
    suspend fun activateByCustomerWallet(
        @Header("Authorization") authHeader: String,
        @Body body: ActivationByCustomerWalletRequest,
    ): ApiResponse<Unit>

    @POST("v1/activation/pin")
    suspend fun setPinCode(
        @Header("Authorization") authHeader: String,
        @Body body: SetPinCodeRequest,
    ): ApiResponse<Unit>

    // endregion

    @GET("customer/info")
    suspend fun getCustomerInfo(
        @Header("Authorization") authHeader: String,
        @Query("card_id") cardId: String,
    ): ApiResponse<VisaCustomerInfo>

    @GET("product_instance/transactions")
    suspend fun getTxHistory(
        @Header("Authorization") authHeader: String,
        @Query("customer_id") customerId: String,
        @Query("product_instance_id") productInstanceId: String,
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
    ): ApiResponse<VisaTxHistoryResponse>

    @GET("v1/customer/kyc")
    suspend fun getKycAccess(@Header("Authorization") authHeader: String): ApiResponse<KycAccessInfoResponse>

    @GET("v1/customer/me")
    suspend fun getCustomerMe(@Header("Authorization") authHeader: String): ApiResponse<CustomerMeResponse>

    @POST("v1/deeplink/validate")
    suspend fun validateDeeplink(@Body body: DeeplinkValidityRequest): ApiResponse<DeeplinkValidityResponse>
}