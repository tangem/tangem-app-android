package com.tangem.spend.datasource.tangempay

import com.tangem.core.remote.response.ApiResponse
import com.tangem.spend.datasource.tangempay.models.request.GenerateNonceByCustomerWalletRequest
import com.tangem.spend.datasource.tangempay.models.request.GetTokenByCustomerWalletRequest
import com.tangem.spend.datasource.tangempay.models.request.RefreshCustomerWalletAccessTokenRequest
import com.tangem.spend.datasource.tangempay.models.response.TangemPayGenerateNonceResponse
import com.tangem.spend.datasource.tangempay.models.response.TangemPayGetTokensResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface TangemPayAuthApi {

    @POST("auth/challenge")
    suspend fun generateNonceByCustomerWallet(
        @Body request: GenerateNonceByCustomerWalletRequest,
    ): ApiResponse<TangemPayGenerateNonceResponse>

    @POST("auth/token")
    suspend fun getTokenByCustomerWallet(
        @Body request: GetTokenByCustomerWalletRequest,
    ): ApiResponse<TangemPayGetTokensResponse>

    @POST("auth/token/refresh")
    suspend fun refreshCustomerWalletAccessToken(
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: RefreshCustomerWalletAccessTokenRequest,
    ): ApiResponse<TangemPayGetTokensResponse>
}