package com.tangem.datasource.api.stakekit

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.stakekit.models.request.MultipleYieldBalancesRequestBody
import com.tangem.datasource.api.stakekit.models.request.RevenueOption
import com.tangem.datasource.api.stakekit.models.request.YieldType
import com.tangem.datasource.api.stakekit.models.response.EnabledYieldsResponse
import com.tangem.datasource.api.stakekit.models.response.model.TokenWithYield
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalances
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

    @GET("yields/balances")
    fun getMultipleYieldBalances(@Body body: List<MultipleYieldBalancesRequestBody>): ApiResponse<List<YieldBalances>>

    @GET("tokens")
    fun getTokens(): ApiResponse<List<TokenWithYield>>
}
