package com.tangem.datasource.api.visa

import com.tangem.datasource.api.visa.models.response.GenerateNonceResponse
import com.tangem.datasource.api.visa.models.response.JWTResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface TangemVisaAuthApi {

    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("auth/clients/mobile-app-android/nonce-challenge")
    suspend fun generateNonceByWalletAddress(
        @Field("customer_id") customerId: String? = null,
        @Field("customer_wallet_address") customerWalletAddress: String,
    ): GenerateNonceResponse

    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("auth/clients/mobile-app-android/nonce-challenge")
    suspend fun generateNonceByCard(
        @Field("card_id") cardId: String,
        @Field("card_public_key") cardPublicKey: String,
    ): GenerateNonceResponse

    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("auth/protocol/openid-connect/token")
    suspend fun getAccessToken(
        @Field("client_id") clientId: String = "mobile-app-android",
        @Field("grant_type") grantType: String = "password",
        @Field("session_id") sessionId: String,
        @Field("signature") signature: String,
        @Field("salt") salt: String?,
    ): JWTResponse

    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("auth/protocol/openid-connect/token")
    suspend fun refreshAccessToken(
        @Field("client_id") clientId: String = "mobile-app-android",
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
    ): JWTResponse
}