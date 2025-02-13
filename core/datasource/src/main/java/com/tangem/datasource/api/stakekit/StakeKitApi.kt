package com.tangem.datasource.api.stakekit

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.stakekit.models.request.*
import com.tangem.datasource.api.stakekit.models.response.EnabledYieldsResponse
import com.tangem.datasource.api.stakekit.models.response.ActionDTO
import com.tangem.datasource.api.stakekit.models.response.GetActionsResponse
import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingGasEstimateDTO
import com.tangem.datasource.api.stakekit.models.response.model.transaction.StakingTransactionDTO
import retrofit2.http.*

@Suppress("LongParameterList")
interface StakeKitApi {

    @GET("yields/enabled")
    suspend fun getEnabledYields(
        @Query("preferredValidatorsOnly") preferredValidatorsOnly: Boolean? = null,
        @Query("ledgerWalletAPICompatible") ledgerWalletAPICompatible: Boolean? = null,
        @Query("type") type: YieldType? = null,
        @Query("revenueOption") revenueOption: RevenueOption? = null,
        @Query("page") page: Int? = null,
        @Query("network") network: String? = null,
        @Query("limit") limit: Int? = null,
    ): ApiResponse<EnabledYieldsResponse>

    @POST("yields/balances")
    suspend fun getMultipleYieldBalances(
        @Body body: List<YieldBalanceRequestBody>,
    ): ApiResponse<Set<YieldBalanceWrapperDTO>>

    @POST("yields/{integrationId}/balances")
    suspend fun getSingleYieldBalance(
        @Path("integrationId") integrationId: String,
        @Body body: YieldBalanceRequestBody,
    ): ApiResponse<List<BalanceDTO>>

    @GET("actions")
    suspend fun getActions(
        @Query("walletAddress") walletAddress: String,
        @Query("network") network: String,
        @Query("status") status: String,
        @Query("sort") sort: String = "createdAtDesc",
        @Query("limit") limit: Int = 50,
    ): ApiResponse<GetActionsResponse>

    @POST("actions/enter")
    suspend fun createEnterAction(@Body body: ActionRequestBody): ApiResponse<ActionDTO>

    @POST("actions/exit")
    suspend fun createExitAction(@Body body: ActionRequestBody): ApiResponse<ActionDTO>

    @POST("actions/pending")
    suspend fun createPendingAction(@Body body: PendingActionRequestBody): ApiResponse<ActionDTO>

    @POST("actions/enter/estimate-gas")
    suspend fun estimateGasOnEnter(@Body body: ActionRequestBody): ApiResponse<StakingGasEstimateDTO>

    @POST("actions/exit/estimate-gas")
    suspend fun estimateGasOnExit(@Body body: ActionRequestBody): ApiResponse<StakingGasEstimateDTO>

    @POST("actions/pending/estimate-gas")
    suspend fun estimateGasOnPending(@Body body: PendingActionRequestBody): ApiResponse<StakingGasEstimateDTO>

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

    @GET("transactions/{transactionId}")
    suspend fun getTransaction(@Path("transactionId") transactionId: String): ApiResponse<StakingTransactionDTO>
}