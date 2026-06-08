package com.tangem.datasource.api.gasless

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.gasless.models.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * v2 of the gasless transaction service ([REDACTED_TASK_KEY]).
 *
 * v2 introduces a per-call `gasLimit` in the `Transaction` struct (changing the EIP-712 typehash) and a
 * batch-sign endpoint. Only the signing endpoints are versioned — `tokens` and `config/fee-recipient` are
 * version-agnostic and keep their `api/v1` paths. The legacy [GaslessTxServiceApi] is left intact for v1.
 */
interface GaslessTxServiceApiV2 {

    @GET("api/v1/tokens")
    suspend fun getSupportedTokens(): ApiResponse<GaslessServiceResponse<GaslessSupportedTokens>>

    @POST("api/v2/transaction/sign")
    suspend fun signGaslessTransaction(
        @Body transaction: GaslessTransactionRequest,
    ): ApiResponse<GaslessServiceResponse<GaslessSignedTransactionResultDTO>>

    @POST("api/v2/transaction/batch-sign")
    suspend fun signGaslessBatchTransaction(
        @Body transaction: GaslessBatchTransactionRequest,
    ): ApiResponse<GaslessServiceResponse<GaslessSignedTransactionResultDTO>>

    @GET("api/v1/config/fee-recipient")
    suspend fun getFeeRecipient(): ApiResponse<GaslessServiceResponse<GaslessFeeRecipient>>
}