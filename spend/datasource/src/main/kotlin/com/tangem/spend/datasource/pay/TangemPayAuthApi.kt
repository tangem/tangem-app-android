package com.tangem.spend.datasource.pay

import com.tangem.core.remote.response.ApiResponse
import com.tangem.spend.datasource.pay.models.request.GenerateNonceByCustomerWalletRequest
import com.tangem.spend.datasource.pay.models.request.GetTokenByCustomerWalletRequest
import com.tangem.spend.datasource.pay.models.request.RefreshCustomerWalletAccessTokenRequest
import com.tangem.spend.datasource.pay.models.response.TangemPayGenerateNonceResponse
import com.tangem.spend.datasource.pay.models.response.TangemPayGetTokensResponse
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