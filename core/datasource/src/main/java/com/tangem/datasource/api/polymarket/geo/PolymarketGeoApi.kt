package com.tangem.datasource.api.polymarket.geo

import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.polymarket.geo.models.PolymarketGeoblockResponse
import retrofit2.http.GET

interface PolymarketGeoApi {

    @GET("api/geoblock")
    suspend fun getGeoblock(): ApiResponse<PolymarketGeoblockResponse>
}