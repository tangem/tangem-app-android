package com.tangem.grow.datasource.polymarket.geo

import com.tangem.core.remote.response.ApiResponse
import okhttp3.ResponseBody
import retrofit2.http.GET

interface PolymarketGeoApi {

    @GET("api/geoblock")
    suspend fun getGeoblock(): ApiResponse<ResponseBody>
}