package com.tangem.datasource.api.ethpool

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolBroadcastRequest
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolDepositRequest
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolUnstakeRequest
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolWithdrawRequest
import com.tangem.datasource.api.ethpool.models.response.*
import retrofit2.http.*

/**
 * P2P.org Ethereum Pooled Staking API client
 *
 * Documentation: https://docs.p2p.org/
 *
 * Base URL: https://api.p2p.org (prod) / https://api-test.p2p.org (testnet)
 */
interface P2PEthPoolApi {

    /**
     * Get list of available vaults
     *
     * @param network Ethereum pool network: "mainnet" or "hoodi" (testnet)
     */
    @GET("api/v1/staking/pool/{network}/vaults")
    suspend fun getVaults(
        @Path("network") network: String = "mainnet",
    ): ApiResponse<P2PEthPoolResponse<P2PEthPoolVaultsResponse>>

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
        @Body body: P2PEthPoolDepositRequest,
    ): ApiResponse<P2PEthPoolResponse<P2PEthPoolDepositResponse>>

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
        @Body body: P2PEthPoolUnstakeRequest,
    ): ApiResponse<P2PEthPoolResponse<P2PEthPoolUnstakeResponse>>

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
        @Body body: P2PEthPoolWithdrawRequest,
    ): ApiResponse<P2PEthPoolResponse<P2PEthPoolWithdrawResponse>>

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
        @Body body: P2PEthPoolBroadcastRequest,
    ): ApiResponse<P2PEthPoolResponse<P2PEthPoolBroadcastResponse>>

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
    ): ApiResponse<P2PEthPoolResponse<P2PEthPoolAccountResponse>>

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
    ): ApiResponse<P2PEthPoolResponse<P2PEthPoolRewardsResponse>>
}