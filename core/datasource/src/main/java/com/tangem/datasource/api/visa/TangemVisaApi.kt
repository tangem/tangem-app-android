package com.tangem.datasource.api.visa

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.visa.models.request.ActivationByCardWalletRequest
import com.tangem.datasource.api.visa.models.request.ActivationByCustomerWalletRequest
import com.tangem.datasource.api.visa.models.request.ActivationStatusRequest
import com.tangem.datasource.api.visa.models.request.ExchangeAccessTokenRequest
import com.tangem.datasource.api.visa.models.request.GenerateNoneByCardIdRequest
import com.tangem.datasource.api.visa.models.request.GenerateNoneByCardWalletRequest
import com.tangem.datasource.api.visa.models.request.GetAccessTokenByCardIdRequest
import com.tangem.datasource.api.visa.models.request.GetAccessTokenByCardWalletRequest
import com.tangem.datasource.api.visa.models.request.GetCardWalletAcceptanceRequest
import com.tangem.datasource.api.visa.models.request.GetCustomerWalletAcceptanceRequest
import com.tangem.datasource.api.visa.models.request.RefreshTokenByCardIdRequest
import com.tangem.datasource.api.visa.models.request.RefreshTokenByCardWalletRequest
import com.tangem.datasource.api.visa.models.request.SetPinCodeRequest
import com.tangem.datasource.api.visa.models.response.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TangemVisaApi {

    // region: auth

    @POST("v1/auth/challenge")
    suspend fun generateNonceByCardId(@Body request: GenerateNoneByCardIdRequest): ApiResponse<GenerateNonceResponse>

    @POST("v1/auth/challenge")
    suspend fun generateNonceByCardWallet(
        @Body request: GenerateNoneByCardWalletRequest,
    ): ApiResponse<GenerateNonceResponse>

    @POST("v1/auth/token")
    suspend fun getAccessTokenByCardId(@Body request: GetAccessTokenByCardIdRequest): ApiResponse<JWTResponse>

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
}