package com.tangem.datasource.api.stakekit

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.request.RevenueOption
import com.tangem.datasource.api.stakekit.models.request.YieldType
import com.tangem.datasource.api.stakekit.models.response.EnabledYieldsResponse
import com.tangem.datasource.api.stakekit.models.response.model.TokenWithYieldDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Suppress("LongParameterList")
interface StakeKitApi {

    @GET("yields/enabled")
    suspend fun getMultipleYields(
        @Query("ledgerWalletAPICompatible") ledgerWalletAPICompatible: Boolean? = null,
        @Query("type") type: YieldType? = null,
        @Query("revenueOption") revenueOption: RevenueOption? = null,
        @Query("page") page: Int? = null,
        @Query("network") network: String? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<EnabledYieldsResponse>

    @GET("yields/{integrationId}")
    suspend fun getSingleYield(
        @Path("integrationId") integrationId: String,
        @Query("ledgerWalletAPICompatible") ledgerWalletAPICompatible: Boolean = false,
    ): ApiResponse<YieldDTO>

    @GET("yields/balances")
    suspend fun getMultipleYieldBalances(
        @Body body: List<YieldBalanceRequestBody>,
    ): ApiResponse<List<YieldBalanceWrapperDTO>>

    @GET("yields/{integrationId}/balances")
    suspend fun getSingleYieldBalance(
        @Path("integrationId") integrationId: String,
        @Body body: YieldBalanceRequestBody,
    ): ApiResponse<YieldBalanceWrapperDTO>

    @GET("tokens")
    suspend fun getTokens(): ApiResponse<List<TokenWithYieldDTO>>
}