package com.tangem.datasource.api.markets

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.markets.models.response.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TangemTechMarketsApi {

    @Suppress("LongParameterList")
    @GET("coins/list")
    suspend fun getCoinsList(
        @Query("currency") currency: String,
        @Query("interval") interval: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("order") order: String,
        @Query("search") search: String?,
        @Query("timestamp") timestamp: Long?,
    ): ApiResponse<TokenMarketListResponse>

    @GET("coins/{coin_id}")
    suspend fun getCoinMarketData(
        @Path("coin_id") coinId: String,
        @Query("currency") currency: String,
        @Query("language") language: String,
    ): ApiResponse<TokenMarketInfoResponse>

    @GET("coins/{coin_id}/history")
    suspend fun getCoinChart(
        @Path("coin_id") coinId: String,
        @Query("currency") currency: String,
        @Query("interval") interval: String,
    ): ApiResponse<TokenMarketChartResponse>

    @GET("coins/{coin_id}/exchanges")
    suspend fun getCoinExchanges(@Path("coin_id") coinId: String): ApiResponse<TokenMarketExchangesResponse>

    @GET("coins/history_preview")
    suspend fun getCoinsListCharts(
        @Query("coin_ids") coinIds: String,
        @Query("currency") currency: String,
        @Query("interval") interval: String,
    ): ApiResponse<TokenMarketChartListResponse>
}
