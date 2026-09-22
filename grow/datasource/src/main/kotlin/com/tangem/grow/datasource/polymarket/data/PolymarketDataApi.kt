package com.tangem.grow.datasource.polymarket.data

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.polymarket.data.models.PolymarketPositionsValueResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PolymarketDataApi {

    /**
     * @param user deposit wallet address whose open positions are valued
     * @return one entry per known user; empty when the wallet holds no positions
     */
    @GET("value")
    suspend fun getPositionsValue(@Query("user") user: String): ApiResponse<List<PolymarketPositionsValueResponse>>
}