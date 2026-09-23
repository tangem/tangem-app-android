package com.tangem.grow.datasource.yield

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.yield.models.YieldMarketsResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldAprHistoryResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimCalldataResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldClaimHistoryResponse
import com.tangem.grow.datasource.yield.models.incentives.YieldIncentivesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YieldIncentivesApi {

    @GET("api/v1/yield-modules/markets")
    suspend fun getIncentiveMarkets(
        @Query("chainId") chainId: String? = null,
        @Query("isForEarn") isForEarn: Boolean? = null,
    ): ApiResponse<YieldMarketsResponse>

    @GET("api/v1/yield-modules/{chainId}/{userAddress}/incentives")
    suspend fun getIncentives(
        @Path("chainId") chainId: Int,
        @Path("userAddress") userAddress: String,
    ): ApiResponse<YieldIncentivesResponse>

    @GET("api/v1/yield-modules/{chainId}/{tokenAddress}/apr-history")
    suspend fun getAprHistory(
        @Path("chainId") chainId: Int,
        @Path("tokenAddress") tokenAddress: String,
        @Query("days") days: Int? = null,
    ): ApiResponse<YieldAprHistoryResponse>

    @GET("api/v1/yield-modules/{chainId}/{userAddress}/claim-calldata")
    suspend fun getClaimCalldata(
        @Path("chainId") chainId: Int,
        @Path("userAddress") userAddress: String,
    ): ApiResponse<YieldClaimCalldataResponse>

    @GET("api/v1/yield-modules/{chainId}/{userAddress}/claim-history")
    suspend fun getClaimHistory(
        @Path("chainId") chainId: Int,
        @Path("userAddress") userAddress: String,
    ): ApiResponse<YieldClaimHistoryResponse>
}