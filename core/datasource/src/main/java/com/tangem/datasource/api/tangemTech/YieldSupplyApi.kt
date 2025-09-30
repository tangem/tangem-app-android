package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.models.YieldMarketsResponse
import com.tangem.datasource.api.tangemTech.models.YieldTokenStatusResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface YieldSupplyApi {

    @GET("v1/yield/markets")
    suspend fun getYieldMarkets(): ApiResponse<YieldMarketsResponse>

    @GET("v1/yield/token/{tokenAddress}")
    suspend fun getYieldTokenStatus(@Path("tokenAddress") tokenAddress: String): ApiResponse<YieldTokenStatusResponse>
}