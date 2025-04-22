package com.tangem.datasource.api.visa

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.visa.models.request.*
import com.tangem.datasource.api.visa.models.response.GenerateNonceResponse
import com.tangem.datasource.api.visa.models.response.JWTResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface TangemVisaAuthApi {

    @POST("v1/auth/challenge")
    suspend fun generateNonceByCardId(@Body request: GenerateNoneByCardIdRequest): GenerateNonceResponse

    @POST("v1/auth/challenge")
    suspend fun generateNonceByCardWallet(@Body request: GenerateNoneByCardWalletRequest): GenerateNonceResponse

    @POST("v1/auth/token")
    suspend fun getAccessTokenByCardId(@Body request: GetAccessTokenByCardIdRequest): JWTResponse

    @POST("v1/auth/token")
    suspend fun getAccessTokenByCardWallet(@Body request: GetAccessTokenByCardWalletRequest): JWTResponse

    @POST("v1/auth/token/refresh")
    suspend fun refreshCardIdAccessToken(@Body request: RefreshTokenByCardIdRequest): ApiResponse<JWTResponse>

    @POST("v1/auth/token/refresh")
    suspend fun refreshCardWalletAccessToken(@Body request: RefreshTokenByCardWalletRequest): ApiResponse<JWTResponse>
}