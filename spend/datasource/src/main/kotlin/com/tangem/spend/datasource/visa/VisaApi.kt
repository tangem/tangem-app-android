package com.tangem.spend.datasource.visa

import com.tangem.core.remote.response.ApiResponse
import com.tangem.spend.datasource.pay.models.request.RefreshTokenByCardIdRequest
import com.tangem.spend.datasource.pay.models.request.SetPinCodeRequest
import com.tangem.spend.datasource.visa.models.request.ActivationByCardWalletRequest
import com.tangem.spend.datasource.visa.models.request.ActivationByCustomerWalletRequest
import com.tangem.spend.datasource.visa.models.request.ActivationStatusRequest
import com.tangem.spend.datasource.visa.models.request.ExchangeAccessTokenRequest
import com.tangem.spend.datasource.visa.models.request.GenerateNoneByCardIdRequest
import com.tangem.spend.datasource.visa.models.request.GenerateNoneByCardWalletRequest
import com.tangem.spend.datasource.visa.models.request.GetAccessTokenByCardIdRequest
import com.tangem.spend.datasource.visa.models.request.GetAccessTokenByCardWalletRequest
import com.tangem.spend.datasource.visa.models.request.GetCardWalletAcceptanceRequest
import com.tangem.spend.datasource.visa.models.request.GetCustomerWalletAcceptanceRequest
import com.tangem.spend.datasource.visa.models.response.CardActivationRemoteStateResponse
import com.tangem.spend.datasource.visa.models.response.GenerateNonceResponse
import com.tangem.spend.datasource.visa.models.response.JWTResponse
import com.tangem.spend.datasource.visa.models.response.VisaDataToSignResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface VisaApi {

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

    @POST("v1/auth/token/exchange")
    suspend fun exchangeAccessToken(@Body request: ExchangeAccessTokenRequest): ApiResponse<JWTResponse>

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
}