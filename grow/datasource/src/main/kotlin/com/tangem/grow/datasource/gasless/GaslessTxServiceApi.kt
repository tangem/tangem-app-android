package com.tangem.grow.datasource.gasless

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.gasless.models.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GaslessTxServiceApi {

    @GET("api/v1/tokens")
    suspend fun getSupportedTokens(): ApiResponse<GaslessServiceResponse<GaslessSupportedTokens>>

    @POST("api/v1/transaction/sign")
    suspend fun signGaslessTransaction(
        @Body transaction: GaslessTransactionRequest,
    ): ApiResponse<GaslessServiceResponse<GaslessSignedTransactionResultDTO>>

    @GET("api/v1/config/fee-recipient")
    suspend fun getFeeRecipient(): ApiResponse<GaslessServiceResponse<GaslessFeeRecipient>>
}