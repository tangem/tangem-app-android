package com.tangem.datasource.local.token

import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import kotlinx.coroutines.flow.Flow

/**
 * Store for P2P Ethereum pooled staking vaults
 * (similar to StakingYieldsStore for StakeKit yields)
 *
 * Vault is ETH-specific concept for pooled staking.
 * For other blockchains, P2PEthPool may use different structures.
 */
interface P2PEthPoolVaultsStore {

    /**
     * Get all stored vaults as Flow
     */
    fun get(): Flow<List<P2PEthPoolVault>>

    /**
     * Get all stored vaults synchronously
     */
    suspend fun getSync(): List<P2PEthPoolVault>

    /**
     * Store vaults from P2PEthPool API
     */
    suspend fun store(vaults: List<P2PEthPoolVault>)
}