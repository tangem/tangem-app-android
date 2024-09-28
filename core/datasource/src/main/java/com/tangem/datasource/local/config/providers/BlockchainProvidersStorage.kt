package com.tangem.datasource.local.config.providers

import com.tangem.datasource.local.config.providers.models.ProviderModel

/**
 * Blockchain providers storage
 *
 * @author Andrew Khokhlov on 27/09/2024
 */
interface BlockchainProvidersStorage {

    /** Get config */
    suspend fun getConfigSync(): Map<String, List<ProviderModel>>
}
