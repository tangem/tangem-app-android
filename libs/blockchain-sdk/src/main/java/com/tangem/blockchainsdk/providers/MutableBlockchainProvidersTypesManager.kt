package com.tangem.blockchainsdk.providers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType

/**
 * Mutable blockchain providers types manager
 *
[REDACTED_AUTHOR]
 */
interface MutableBlockchainProvidersTypesManager : BlockchainProvidersTypesManager {

    /** Update [providers] for [blockchain] */
    suspend fun update(blockchain: Blockchain, providers: List<ProviderType>)

    /** Recover initial state */
    suspend fun recoverInitialState()

    /** Checks that current [BlockchainProviderTypes] was changed */
    suspend fun isMatchWithMerged(): Boolean
}