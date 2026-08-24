package com.tangem.grow.datasource.gasless

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.gasless.models.GaslessServiceResponse
import com.tangem.grow.datasource.gasless.models.tron.TronEstimateRequestBody
import com.tangem.grow.datasource.gasless.models.tron.TronEstimateResponse
import com.tangem.grow.datasource.gasless.models.tron.TronSubmitRequestBody
import com.tangem.grow.datasource.gasless.models.tron.TronSubmitResponse
import com.tangem.grow.datasource.gasless.models.tron.TronTokensResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface TronGaslessApi {

    @GET("api/v1/tron/tokens")
    suspend fun getSupportedTokens(): ApiResponse<GaslessServiceResponse<TronTokensResponse>>

    @POST("api/v1/tron/transaction/estimate")
    suspend fun estimate(@Body body: TronEstimateRequestBody): ApiResponse<GaslessServiceResponse<TronEstimateResponse>>

    @POST("api/v1/tron/transaction/submit")
    suspend fun submit(@Body body: TronSubmitRequestBody): ApiResponse<GaslessServiceResponse<TronSubmitResponse>>
}