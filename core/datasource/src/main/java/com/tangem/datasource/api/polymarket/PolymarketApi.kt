package com.tangem.datasource.api.polymarket

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.polymarket.models.PolymarketEventsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Polymarket predictions BFF Discovery API (`api/predictions/v1`).
 *
 * Provided in [com.tangem.datasource.di.NetworkModule] via the [ApiConfig.ID.Predictions] config.
 */
interface PolymarketApi {

    /**
     * Discovery feed: paginated prediction events, each with its top active markets.
     *
     * @param limit page size (BFF default 20)
     * @param cursor keyset pagination cursor; `null` for the first page
     */
    @GET("api/predictions/v1/events")
    suspend fun getEvents(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String?,
    ): ApiResponse<PolymarketEventsResponse>
}