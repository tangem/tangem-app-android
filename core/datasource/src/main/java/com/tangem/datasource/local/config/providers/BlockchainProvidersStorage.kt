package com.tangem.datasource.local.config.providers

import com.tangem.datasource.local.config.providers.models.ProviderModel

/**
 * Blockchain providers storage
 *
[REDACTED_AUTHOR]
 */
interface BlockchainProvidersStorage {

    /** Get config */
    suspend fun getConfigSync(): Map<String, List<ProviderModel>>
}