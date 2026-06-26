package com.tangem.datasource.api.marketing

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.marketing.models.MarketingCampaignsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface MarketingApi {

    @GET("v1/marketing/campaigns")
    suspend fun getCampaigns(
        @Query("type") type: String,
        @Query("language") language: String? = null,
        @Query("fromNetwork") fromNetwork: String? = null,
        @Query("fromContractAddress") fromContractAddress: String? = null,
        @Query("toNetwork") toNetwork: String? = null,
        @Query("toContractAddress") toContractAddress: String? = null,
        @Query("fromFiat") fromFiat: String? = null,
        @Query("toToken") toToken: String? = null,
        @Query("tokenId") tokenId: String? = null,
        @Header("If-None-Match") eTag: String? = null,
    ): ApiResponse<MarketingCampaignsResponse>
}