package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.models.v2.UserTokensResponseV2
import retrofit2.http.*

interface TangemTechApiV2 {

    @GET("user-tokens/{wallet_id}")
    suspend fun getUserTokens(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Path("wallet_id") walletId: String,
    ): ApiResponse<UserTokensResponseV2>

    @PUT("user-tokens/{wallet_id}")
    suspend fun saveUserTokens(
        @Header("card_public_key") cardPublicKey: String,
        @Header("card_id") cardId: String,
        @Path("wallet_id") walletId: String,
        @Body userTokens: UserTokensResponseV2,
    ): ApiResponse<Unit>
}