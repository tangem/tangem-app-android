package com.tangem.blockchainsdk.providers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.network.providers.ProviderType

/**
 * Mutable blockchain providers types manager
 *
 * @author Andrew Khokhlov on 06/01/2025
 */
interface MutableBlockchainProvidersTypesManager : BlockchainProvidersTypesManager {

    /** Update [providers] for [blockchain] */
    suspend fun update(blockchain: Blockchain, providers: List<ProviderType>)

    /** Checks that current [BlockchainProviderTypes] was changed */
    suspend fun isMatchWithMerged(): Boolean
}
