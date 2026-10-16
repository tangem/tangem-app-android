package com.tangem.grow.datasource.polymarket.data

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.polymarket.data.models.PolymarketPositionsValueResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface PolymarketDataApi {

    /**
     * @param user deposit wallet address whose open positions are valued
     * @return the wallet's total, zero when it holds no positions
     */
    @GET("v2/value")
    suspend fun getPositionsValue(@Query("user") user: String): ApiResponse<PolymarketPositionsValueResponse>
}