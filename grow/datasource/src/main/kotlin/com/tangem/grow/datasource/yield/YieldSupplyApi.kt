package com.tangem.grow.datasource.yield

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.yield.models.YieldMarketsResponse
import com.tangem.grow.datasource.yield.models.YieldModuleStatusResponse
import com.tangem.grow.datasource.yield.models.YieldSupplyChangeTokenStatusBody
import com.tangem.grow.datasource.yield.models.YieldSupplyMarketTokenDto
import com.tangem.grow.datasource.yield.models.YieldTokenChartResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface YieldSupplyApi {

    @GET("api/v1/yield/markets")
    suspend fun getYieldMarkets(@Query("chainId") chainId: String? = null): ApiResponse<YieldMarketsResponse>

    @GET("api/v1/yield/token/{chainId}/{tokenAddress}")
    suspend fun getYieldTokenStatus(
        @Path("chainId") chainId: Int,
        @Path("tokenAddress") tokenAddress: String,
    ): ApiResponse<YieldSupplyMarketTokenDto>

    @GET("api/v1/yield/token/{chainId}/{tokenAddress}/chart")
    suspend fun getYieldTokenChart(
        @Path("chainId") chainId: Int,
        @Path("tokenAddress") tokenAddress: String,
        @Query("window") window: String? = null,
        @Query("bucketSizeDays") bucketSizeDays: Int? = null,
    ): ApiResponse<YieldTokenChartResponse>

    @POST("api/v1/module/activate")
    suspend fun activateYieldModule(
        @Body body: YieldSupplyChangeTokenStatusBody,
        @Header("userWalletId") userWalletId: String,
    ): ApiResponse<YieldModuleStatusResponse>

    @POST("api/v1/module/deactivate")
    suspend fun deactivateYieldModule(
        @Body body: YieldSupplyChangeTokenStatusBody,
    ): ApiResponse<YieldModuleStatusResponse>
}