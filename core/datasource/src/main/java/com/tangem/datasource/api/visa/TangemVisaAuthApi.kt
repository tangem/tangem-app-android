package com.tangem.datasource.api.visa

import com.tangem.datasource.api.visa.models.response.GenerateNonceResponse
import com.tangem.datasource.api.visa.models.response.JWTResponse
import retrofit2.http.Field
import retrofit2.http.POST
import retrofit2.http.Query

interface TangemVisaAuthApi {

    @POST("auth/card_wallet")
    suspend fun generateNonceByWalletAddress(
        @Query("card_wallet_address") cardWalletAddress: String,
    ): GenerateNonceResponse

    @POST("auth/card_id")
    suspend fun generateNonceByCard(
        @Query("card_id") cardId: String,
        @Query("card_public_key") cardPublicKey: String,
    ): GenerateNonceResponse

    @POST("auth/get_token")
    suspend fun getAccessToken(
        @Query("session_id") sessionId: String,
        @Query("signature") signature: String,
        @Query("salt") salt: String?,
    ): JWTResponse

    @POST("auth/refresh_token")
    suspend fun refreshAccessToken(@Field("refresh_token") refreshToken: String): JWTResponse
}