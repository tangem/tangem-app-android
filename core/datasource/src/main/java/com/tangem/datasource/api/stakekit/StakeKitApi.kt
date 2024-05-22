package com.tangem.datasource.api.stakekit

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.stakekit.models.request.AddressWithIntegrationId
import com.tangem.datasource.api.stakekit.models.request.RevenueOption
import com.tangem.datasource.api.stakekit.models.request.YieldType
import com.tangem.datasource.api.stakekit.models.response.EnabledYieldsResponse
import com.tangem.datasource.api.stakekit.models.response.YieldBalances
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Query

@Suppress("LongParameterList")
interface StakeKitApi {

    @GET("yields/enabled")
    fun getAllYields(
        @Query("ledgerWalletAPICompatible") ledgerWalletAPICompatible: Boolean,
        @Query("type") type: YieldType,
        @Query("revenueOption") revenueOption: RevenueOption,
        @Query("page") page: Int,
        @Query("network") network: String,
        @Query("limit") limit: Int,
    ): ApiResponse<EnabledYieldsResponse>

    @GET("yields/enabled")
    fun getMultipleYieldBalances(
        @Body body: List<AddressWithIntegrationId>
    ): ApiResponse<List<YieldBalances>>
}
