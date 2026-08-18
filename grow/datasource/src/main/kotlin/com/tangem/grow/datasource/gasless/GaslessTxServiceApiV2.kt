package com.tangem.grow.datasource.gasless

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.gasless.models.GaslessBatchTransactionRequest
import com.tangem.grow.datasource.gasless.models.GaslessServiceResponse
import com.tangem.grow.datasource.gasless.models.GaslessSignedTransactionResultDTO
import com.tangem.grow.datasource.gasless.models.GaslessTransactionRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface GaslessTxServiceApiV2 {
    @POST("api/v2/transaction/sign")
    suspend fun signGaslessTransaction(
        @Body transaction: GaslessTransactionRequest,
    ): ApiResponse<GaslessServiceResponse<GaslessSignedTransactionResultDTO>>

    @POST("api/v2/transaction/batch-sign")
    suspend fun signGaslessBatchTransaction(
        @Body transaction: GaslessBatchTransactionRequest,
    ): ApiResponse<GaslessServiceResponse<GaslessSignedTransactionResultDTO>>
}