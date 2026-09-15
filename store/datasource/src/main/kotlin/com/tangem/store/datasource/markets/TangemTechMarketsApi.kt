package com.tangem.store.datasource.markets

import com.tangem.core.remote.response.ApiResponse
import com.tangem.store.datasource.markets.models.response.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TangemTechMarketsApi {

    @Suppress("LongParameterList")
    @GET("v1/coins/list")
    suspend fun getCoinsList(
        @Query("currency") currency: String,
        @Query("interval") interval: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("order") order: String,
        @Query("search") search: String?,
        @Query("timestamp") timestamp: Long?,
        @Query("showNetworks") showNetworks: Boolean? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("sectorId") sectorId: String? = null,
        @Query("geckoCategoryId") geckoCategoryId: String? = null,
        /** ISO 3166-1 alpha-2 the `restricted_for_country` flag is evaluated for; omitting it leaves every
         * token unrestricted. Undocumented in the categories spec, confirmed against the live endpoint. */
        @Query("country") country: String? = null,
    ): ApiResponse<TokenMarketListResponse>

    @GET("v1/coins/{coin_id}")
    suspend fun getCoinMarketData(
        @Path("coin_id") coinId: String,
        @Query("currency") currency: String,
        @Query("language") language: String,
    ): ApiResponse<TokenMarketInfoResponse>

    @GET("v1/coins/{coin_id}/history")
    suspend fun getCoinChart(
        @Path("coin_id") coinId: String,
        @Query("currency") currency: String,
        @Query("interval") interval: String,
    ): ApiResponse<TokenMarketChartResponse>

    @GET("v1/coins/{coin_id}/exchanges")
    suspend fun getCoinExchanges(@Path("coin_id") coinId: String): ApiResponse<TokenMarketExchangesResponse>

    @GET("v1/coins/history_preview")
    suspend fun getCoinsListCharts(
        @Query("coin_ids") coinIds: String,
        @Query("currency") currency: String,
        @Query("interval") interval: String,
    ): ApiResponse<TokenMarketChartListResponse>

    /**
     * Returns technical and social indicator readings for the curated coin set
     *
     * @param symbols    comma-separated coin symbols (case-insensitive, e.g. `BTC,ETH`).
     *                   Defaults to all supported coins
     * @param indicators comma-separated indicator types (e.g. `rsi,macd`). Defaults to all
     */
    @GET("api/v1/coins/indicators")
    suspend fun getCoinIndicators(
        @Query("symbols") symbols: String? = null,
        @Query("indicators") indicators: String? = null,
    ): ApiResponse<GetCoinIndicatorsResponse>

    /**
     * Returns the coin categories tree — each category with its sectors and coin counts
     *
     * @param country ISO 3166-1 alpha-2 code the `restricted` flag is evaluated for. It narrows nothing:
     *                omitting it returns the very same categories, all with `restricted = false`
     */
    @GET("v1/coins/categories")
    suspend fun getCoinCategories(@Query("country") country: String? = null): ApiResponse<CoinCategoriesResponse>

    /**
     * Returns a page of market categories with their aggregated market data
     *
     * @param timestamp pagination snapshot token: pass the same value for every page of one listing
     *                  so it stays stable while paging. The response does not echo it back
     * @param featured  when `true`, only categories curated as featured are returned
     * @param search    narrows the page to categories matching the query; `null` returns all of them
     */
    @GET("v1/coins/gecko-categories")
    suspend fun getMarketCategories(
        @Query("currency") currency: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("timestamp") timestamp: Long? = null,
        @Query("featured") featured: Boolean? = null,
        @Query("search") search: String? = null,
    ): ApiResponse<MarketCategoriesResponse>

    /**
     * Returns the global market indicators of the "Total market cap" block: total capitalisation
     * with its 24h change, the Fear & Greed index and the Altcoin Season index.
     *
     * The capitalisation is converted server-side into [currency], so a currency switch has to go back
     * to the backend; the index labels are localised by the backend into [language].
     */
    @GET("api/v1/market-indicators")
    suspend fun getMarketIndicators(
        @Query("currency") currency: String,
        @Query("language") language: String,
    ): ApiResponse<MarketIndicatorsResponse>
}