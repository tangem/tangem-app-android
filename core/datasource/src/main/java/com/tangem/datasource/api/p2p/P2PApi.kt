package com.tangem.datasource.api.p2p

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.p2p.models.request.P2PBroadcastTransactionRequestBody
import com.tangem.datasource.api.p2p.models.request.P2PDepositRequestBody
import com.tangem.datasource.api.p2p.models.request.P2PUnstakeRequestBody
import com.tangem.datasource.api.p2p.models.request.P2PWithdrawRequestBody
import com.tangem.datasource.api.p2p.models.response.*
import retrofit2.http.*

/**
 * P2P.org Pooled Staking API client
 *
 * Documentation: https://docs.p2p.org/
 *
 * Base URL: https://api.p2p.org (prod) / https://api-test.p2p.org (testnet)
 */
interface P2PApi {

    /**
     * Get list of available vaults
     *
     * @param network Ethereum pool network: "mainnet" or "hoodi" (testnet)
     */
    @GET("api/v1/staking/pool/{network}/vaults")
    suspend fun getVaults(
        @Path("network") network: String = "mainnet",
    ): ApiResponse<P2PResponseWrapper<P2PVaultsResponse>>

    /**
     * Prepare deposit transaction
     *
     * Create unsigned transaction for depositing ETH into a vault.
     *
     * @param network Ethereum pool network: "mainnet" or "hoodi"
     * @param body Deposit parameters (delegator address, vault address, amount)
     */
    @POST("api/v1/staking/pool/{network}/staking/deposit")
    suspend fun createDepositTransaction(
        @Path("network") network: String,
        @Body body: P2PDepositRequestBody,
    ): ApiResponse<P2PResponseWrapper<P2PDepositResponse>>

    /**
     * Prepare unstake transaction
     *
     * Create unsigned transaction to initiate unstaking process.
     *
     * @param network Ethereum pool network: "mainnet" or "hoodi"
     * @param body Unstake parameters (staker public key, stake transaction hash)
     */
    @POST("api/v1/staking/pool/{network}/staking/unstake")
    suspend fun createUnstakeTransaction(
        @Path("network") network: String,
        @Body body: P2PUnstakeRequestBody,
    ): ApiResponse<P2PResponseWrapper<P2PUnstakeResponse>>

    /**
     * Prepare withdrawal transaction
     *
     * Create unsigned transaction to withdraw available funds from exit queue.
     *
     * @param network Ethereum pool network: "mainnet" or "hoodi"
     * @param body Withdrawal parameters (staker address)
     */
    @POST("api/v1/staking/pool/{network}/staking/withdraw")
    suspend fun createWithdrawTransaction(
        @Path("network") network: String,
        @Body body: P2PWithdrawRequestBody,
    ): ApiResponse<P2PResponseWrapper<P2PWithdrawResponse>>

    /**
     * Broadcast signed transaction
     *
     * Submit a signed transaction to the blockchain network.
     *
     * @param network Ethereum pool network: "mainnet" or "hoodi"
     * @param body Signed transaction in hexadecimal format
     */
    @POST("api/v1/staking/pool/{network}/transaction/send")
    suspend fun broadcastTransaction(
        @Path("network") network: String,
        @Body body: P2PBroadcastTransactionRequestBody,
    ): ApiResponse<P2PResponseWrapper<P2PBroadcastTransactionResponse>>

    /**
     * Get account summary
     *
     * Retrieve staking balance, rewards, and exit queue information for a specific account and vault.
     *
     * @param network Ethereum pool network: "mainnet" or "hoodi"
     * @param delegatorAddress Account address that initiated staking
     * @param vaultAddress Ethereum address of the vault
     */
    @GET("api/v1/staking/pool/{network}/account/{delegatorAddress}/vault/{vaultAddress}")
    suspend fun getAccountInfo(
        @Path("network") network: String,
        @Path("delegatorAddress") delegatorAddress: String,
        @Path("vaultAddress") vaultAddress: String,
    ): ApiResponse<P2PResponseWrapper<P2PAccountInfoResponse>>

    /**
     * Get rewards history
     *
     * Retrieve historical rewards data for a specific account and vault.
     *
     * @param network Ethereum pool network: "mainnet" or "hoodi"
     * @param delegatorAddress Account address that initiated staking
     * @param vaultAddress Ethereum address of the vault
     * @param period Optional period filter (30, 60, or 90 days)
     */
    @GET("api/v1/staking/pool/{network}/account/{delegatorAddress}/vault/{vaultAddress}/rewards")
    suspend fun getRewards(
        @Path("network") network: String,
        @Path("delegatorAddress") delegatorAddress: String,
        @Path("vaultAddress") vaultAddress: String,
        @Query("period") period: Int? = null,
    ): ApiResponse<P2PResponseWrapper<P2PRewardsResponse>>
}