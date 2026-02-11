package com.tangem.domain.staking.repositories

import arrow.core.Either
import com.tangem.domain.models.staking.P2PEthPoolStakingAccount
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastResult
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig
import com.tangem.domain.staking.model.stakekit.StakingError
import kotlinx.coroutines.flow.Flow

interface P2PEthPoolRepository {

    /**
     * Fetch and store available staking vaults
     *
     * @param network P2PEthPool network (MAINNET or TESTNET)
     */
    suspend fun fetchVaults(network: P2PEthPoolNetwork = P2PEthPoolStakingConfig.activeNetwork)

    /**
     * Get list of available staking vaults
     *
     * @param network P2PEthPool network (MAINNET or TESTNET)
     * @return Either error or list of vaults with APY, capacity, fees
     */
    suspend fun getVaults(
        network: P2PEthPoolNetwork = P2PEthPoolStakingConfig.activeNetwork,
    ): Either<StakingError, List<P2PEthPoolVault>>

    /**
     * Create unsigned transaction for depositing ETH into a vault
     *
     * @param network P2PEthPool network (MAINNET or TESTNET)
     * @param delegatorAddress User's wallet address
     * @param vaultAddress Vault contract address
     * @param amount Amount of ETH to deposit
     * @return Either error or unsigned transaction ready to sign
     */
    suspend fun createDepositTransaction(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx>

    /**
     * Create unsigned transaction to initiate unstaking
     *
     * Unstaking adds funds to exit queue. After ~1-4 days, use [createWithdrawTransaction]
     * to withdraw the funds.
     *
     * @param network P2PEthPool network (MAINNET or TESTNET)
     * @param delegatorAddress User's wallet address
     * @param vaultAddress Vault contract address
     * @param amount Amount of ETH to unstake
     * @return Either error or unsigned transaction
     */
    suspend fun createUnstakeTransaction(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx>

    /**
     * Create unsigned transaction to withdraw funds from exit queue
     *
     * Only works when funds are available (after exit queue wait period).
     *
     * @param network P2PEthPool network (MAINNET or TESTNET)
     * @param delegatorAddress User's wallet address
     * @param vaultAddress Vault contract address
     * @param amount Amount of ETH to withdraw
     * @return Either error or unsigned transaction with withdrawal tickets
     */
    suspend fun createWithdrawTransaction(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
        amount: String,
    ): Either<StakingError, P2PEthPoolUnsignedTx>

    /**
     * Broadcast signed transaction to blockchain
     *
     * @param network P2PEthPool network (MAINNET or TESTNET)
     * @param signedTransaction Signed transaction in hex format (with 0x prefix)
     * @return Either error or broadcast result with transaction hash
     */
    suspend fun broadcastTransaction(
        network: P2PEthPoolNetwork,
        signedTransaction: String,
    ): Either<StakingError, P2PEthPoolBroadcastResult>

    /**
     * Get account staking information for specific vault
     *
     * Returns current stake, rewards, exit queue status, and available amounts
     *
     * @param network P2PEthPool network (MAINNET or TESTNET)
     * @param delegatorAddress User's wallet address
     * @param vaultAddress Vault contract address
     * @return Either error or account info
     */
    suspend fun getAccountInfo(
        network: P2PEthPoolNetwork,
        delegatorAddress: String,
        vaultAddress: String,
    ): Either<StakingError, P2PEthPoolStakingAccount>

    /**
     * Get flow of cached vaults.
     *
     * This returns vaults from the local cache/store.
     * Call [fetchVaults] first to populate the cache from the network.
     *
     * @return Flow of cached vaults list
     */
    fun getVaultsFlow(): Flow<List<P2PEthPoolVault>>

    /**
     * Get cached vaults synchronously from local store.
     *
     * This returns vaults from the local cache/store without network call.
     * Call [fetchVaults] first to populate the cache from the network.
     *
     * @return List of cached vaults (empty if cache is not populated)
     */
    suspend fun getVaultsSync(): List<P2PEthPoolVault>

    /**
     * Check P2PEthPool staking availability by finding public vault
     *
     * @return Flow of StakingAvailability - Available with StakingOption.P2PEthPool if public vault found,
     *         TemporaryUnavailable if not found or vaults empty
     */
    fun getStakingAvailability(): Flow<StakingAvailability>

    /**
     * Check P2PEthPool staking availability synchronously
     *
     * @return StakingAvailability - Available with StakingOption.P2PEthPool if public vault found,
     *         TemporaryUnavailable if not found or vaults empty
     */
    suspend fun getStakingAvailabilitySync(): StakingAvailability
}