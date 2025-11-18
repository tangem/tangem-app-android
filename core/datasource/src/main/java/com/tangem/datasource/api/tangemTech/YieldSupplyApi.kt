package com.tangem.datasource.api.tangemTech

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.tangemTech.models.YieldMarketsResponse
import com.tangem.datasource.api.tangemTech.models.YieldModuleStatusResponse
import com.tangem.datasource.api.tangemTech.models.YieldSupplyChangeTokenStatusBody
import com.tangem.datasource.api.tangemTech.models.YieldSupplyMarketTokenDto
import com.tangem.datasource.api.tangemTech.models.YieldTokenChartResponse
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