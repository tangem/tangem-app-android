package com.tangem.datasource.api.gasless

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.gasless.models.GaslessServiceResponse
import com.tangem.datasource.api.gasless.models.GaslessSignedTransactionResult
import com.tangem.datasource.api.gasless.models.GaslessSupportedTokens
import com.tangem.datasource.api.gasless.models.GaslessTransactionRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GaslessTxServiceApi {

    @GET("api/v1/tokens")
    suspend fun getSupportedTokens(): ApiResponse<GaslessServiceResponse<GaslessSupportedTokens>>

    @POST("api/v1/sign")
    suspend fun signGaslessTransaction(
        @Body transaction: GaslessTransactionRequest,
    ): ApiResponse<GaslessServiceResponse<GaslessSignedTransactionResult>>
}