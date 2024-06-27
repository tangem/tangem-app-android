package com.tangem.datasource.api.stakekit

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.request.RevenueOption
import com.tangem.datasource.api.stakekit.models.request.YieldType
import com.tangem.datasource.api.stakekit.models.response.EnabledYieldsResponse
import com.tangem.datasource.api.stakekit.models.response.model.TokenWithYield
import com.tangem.datasource.api.stakekit.models.response.model.Yield
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapper
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Suppress("LongParameterList")
interface StakeKitApi {

    @GET("yields/enabled")
    suspend fun getMultipleYields(
        @Query("ledgerWalletAPICompatible") ledgerWalletAPICompatible: Boolean,
        @Query("type") type: YieldType,
        @Query("revenueOption") revenueOption: RevenueOption,
        @Query("page") page: Int,
        @Query("network") network: String,
        @Query("limit") limit: Int,
    ): ApiResponse<EnabledYieldsResponse>

    @GET("yields/{integrationId}")
    suspend fun getSingleYield(
        @Path("integrationId") integrationId: String,
        @Query("ledgerWalletAPICompatible") ledgerWalletAPICompatible: Boolean = false,
    ): ApiResponse<Yield>

    @GET("yields/balances")
    suspend fun getMultipleYieldBalances(
        @Body body: List<YieldBalanceRequestBody>,
    ): ApiResponse<List<YieldBalanceWrapper>>

    @GET("yields/{integrationId}/balances")
    suspend fun getSingleYieldBalance(
        @Path("integrationId") integrationId: String,
        @Body body: YieldBalanceRequestBody,
    ): ApiResponse<YieldBalanceWrapper>

    @GET("tokens")
    suspend fun getTokens(): ApiResponse<List<TokenWithYield>>
}