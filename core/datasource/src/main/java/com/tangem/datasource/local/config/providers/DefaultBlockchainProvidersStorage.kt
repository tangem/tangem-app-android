package com.tangem.datasource.local.config.providers

import com.tangem.datasource.asset.loader.AssetLoader
import com.tangem.datasource.local.config.providers.models.ProviderModel
import com.tangem.datasource.local.datastore.RuntimeStateStore

/**
 * Default blockchain providers storage
 *
 * @property assetLoader       asset loader
 * @property runtimeStateStore runtime state store
 */
internal class DefaultBlockchainProvidersStorage(
    private val assetLoader: AssetLoader,
    private val runtimeStateStore: RuntimeStateStore<Map<String, List<ProviderModel>>>,
) : BlockchainProvidersStorage {

    override suspend fun getConfigSync(): Map<String, List<ProviderModel>> {
        val cachedData = runtimeStateStore.get().value

        if (cachedData.isNotEmpty()) return cachedData

        val config = assetLoader.load<Map<String, List<ProviderModel>>>(fileName = PROVIDER_TYPES_FILE_NAME).orEmpty()

        runtimeStateStore.store(value = config)

        return config
    }

    private companion object {
        const val PROVIDER_TYPES_FILE_NAME = "tangem-app-config/providers_order"
    }
}