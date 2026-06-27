package com.tangem.datasource.api.gasless

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.gasless.models.GaslessBatchTransactionRequest
import com.tangem.datasource.api.gasless.models.GaslessServiceResponse
import com.tangem.datasource.api.gasless.models.GaslessSignedTransactionResultDTO
import com.tangem.datasource.api.gasless.models.GaslessTransactionRequest
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