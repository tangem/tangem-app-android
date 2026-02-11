package com.tangem.datasource.api.gasless

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.gasless.models.*
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