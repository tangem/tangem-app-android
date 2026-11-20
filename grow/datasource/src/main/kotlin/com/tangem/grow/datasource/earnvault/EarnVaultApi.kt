package com.tangem.grow.datasource.earnvault

import com.tangem.core.remote.response.ApiResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultApyHistoryResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultApyPeriodDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultDepositPrepareBody
import com.tangem.grow.datasource.earnvault.models.EarnVaultMarketsResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultPositionsResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultPrepareResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultRegisterTransactionsBody
import com.tangem.grow.datasource.earnvault.models.EarnVaultTransactionDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultTransactionsResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultUpdateTransactionBody
import com.tangem.grow.datasource.earnvault.models.EarnVaultWithdrawPrepareBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Amounts are strings in the token's smallest unit */
interface EarnVaultApi {

    /** @param token a contract address, or `native` for the network's coin */
    @GET("api/v1/earn/vaults")
    suspend fun getMarkets(
        @Query("network") network: String? = null,
        @Query("token") token: String? = null,
        @Query("vaultId") vaultId: String? = null,
    ): ApiResponse<EarnVaultMarketsResponse>

    @GET("api/v1/earn/vaults/apy-history")
    suspend fun getApyHistory(
        @Query("vaultId") vaultId: String,
        @Query("network") network: String,
        @Query("period") period: EarnVaultApyPeriodDto,
    ): ApiResponse<EarnVaultApyHistoryResponse>

    /** @param refresh skips the backend's 5-minute position cache */
    @GET("api/v1/earn/vaults/positions")
    suspend fun getPositions(
        @Query("address") address: String,
        @Query("network") network: String,
        @Query("vaultId") vaultId: String? = null,
        @Query("refresh") refresh: Boolean? = null,
    ): ApiResponse<EarnVaultPositionsResponse>

    @POST("api/v1/earn/vaults/deposit/prepare")
    suspend fun prepareDeposit(
        @Header("userWalletId") userWalletId: String,
        @Body body: EarnVaultDepositPrepareBody,
    ): ApiResponse<EarnVaultPrepareResponse>

    @POST("api/v1/earn/vaults/withdraw/prepare")
    suspend fun prepareWithdraw(
        @Header("userWalletId") userWalletId: String,
        @Body body: EarnVaultWithdrawPrepareBody,
    ): ApiResponse<EarnVaultPrepareResponse>

    /** Idempotent by `txHash` and `network` */
    @POST("api/v1/earn/vaults/transactions")
    suspend fun registerTransactions(
        @Header("userWalletId") userWalletId: String,
        @Body body: EarnVaultRegisterTransactionsBody,
    ): ApiResponse<List<EarnVaultTransactionDto>>

    /** @param id the transaction hash, or the `batchId` of a gasless batch that has no hash yet */
    @PATCH("api/v1/earn/vaults/transactions/{id}")
    suspend fun updateTransaction(
        @Header("userWalletId") userWalletId: String,
        @Path("id") id: String,
        @Body body: EarnVaultUpdateTransactionBody,
    ): ApiResponse<EarnVaultTransactionDto>

    /** Deposits, withdrawals and standalone unwraps, newest first */
    @GET("api/v1/earn/vaults/transactions")
    suspend fun getTransactions(
        @Query("address") address: String,
        @Query("network") network: String,
        @Query("vaultId") vaultId: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("cursor") cursor: String? = null,
    ): ApiResponse<EarnVaultTransactionsResponse>

    /** @param id the Tangem transaction id from a registration or a push */
    @GET("api/v1/earn/vaults/transactions/{id}")
    suspend fun getTransaction(@Path("id") id: String): ApiResponse<EarnVaultTransactionDto>
}