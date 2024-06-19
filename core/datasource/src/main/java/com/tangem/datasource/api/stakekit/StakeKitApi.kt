package com.tangem.datasource.api.stakekit

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.stakekit.models.request.*
import com.tangem.datasource.api.stakekit.models.response.EnabledYieldsResponse
import com.tangem.datasource.api.stakekit.models.response.EnterActionResponse
import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionDTO
import com.tangem.datasource.api.stakekit.models.response.model.TokenWithYieldDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import retrofit2.http.*

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

    @POST("actions/enter")
    suspend fun createEnterAction(@Body body: EnterActionRequestBody): ApiResponse<EnterActionResponse>

    @PATCH("transactions/{transactionId}")
    suspend fun constructTransaction(
        @Path("transactionId") transactionId: String,
        @Body body: ConstructTransactionRequestBody,
    ): ApiResponse<StakingTransactionDTO>

    @POST("transactions/{transactionId}/submit_hash")
    suspend fun submitTransactionHash(
        @Path("transactionId") transactionId: String,
        @Body body: SubmitTransactionHashRequestBody,
    ): ApiResponse<Unit>
}