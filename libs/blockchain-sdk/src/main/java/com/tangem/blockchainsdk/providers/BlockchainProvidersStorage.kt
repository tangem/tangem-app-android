package com.tangem.blockchainsdk.providers

import com.tangem.blockchainsdk.providers.models.ProviderModel

/**
 * Blockchain providers storage
 *
[REDACTED_AUTHOR]
 */
interface BlockchainProvidersStorage {

    /** Get config */
    suspend fun getConfigSync(): Map<String, List<ProviderModel>>
}