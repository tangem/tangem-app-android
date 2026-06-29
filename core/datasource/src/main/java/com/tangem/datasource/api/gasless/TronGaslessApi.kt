package com.tangem.datasource.api.gasless

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.gasless.models.GaslessServiceResponse
import com.tangem.datasource.api.gasless.models.tron.TronEstimateRequestBody
import com.tangem.datasource.api.gasless.models.tron.TronEstimateResponse
import com.tangem.datasource.api.gasless.models.tron.TronSubmitRequestBody
import com.tangem.datasource.api.gasless.models.tron.TronSubmitResponse
import com.tangem.datasource.api.gasless.models.tron.TronTokensResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TronGaslessApi {

    @GET("tron/tokens")
    suspend fun getSupportedTokens(): ApiResponse<GaslessServiceResponse<TronTokensResponse>>

    @POST("tron/transaction/estimate")
    suspend fun estimate(
        @Body body: TronEstimateRequestBody,
    ): ApiResponse<GaslessServiceResponse<TronEstimateResponse>>

    @POST("tron/transaction/submit")
    suspend fun submit(
        @Body body: TronSubmitRequestBody,
    ): ApiResponse<GaslessServiceResponse<TronSubmitResponse>>
}