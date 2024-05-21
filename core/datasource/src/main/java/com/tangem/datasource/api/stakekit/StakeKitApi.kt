package com.tangem.datasource.api.stakekit

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.express.models.response.Asset
import com.tangem.datasource.api.stakekit.models.request.RevenueOption
import com.tangem.datasource.api.stakekit.models.request.YieldType
import com.tangem.datasource.api.stakekit.models.response.YieldsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface StakeKitApi {

    @GET("v1/yields")
    fun getAllYields(
        @Query("ledgerWalletAPICompatible") ledgerWalletAPICompatible: Boolean,
        @Query("type") type: YieldType,
        @Query("revenueOption") revenueOption: RevenueOption,
        @Query("page") page: Int,
        @Query("network") network: String,
        @Query("limit") limit: Int
    ): ApiResponse<YieldsResponse>

}