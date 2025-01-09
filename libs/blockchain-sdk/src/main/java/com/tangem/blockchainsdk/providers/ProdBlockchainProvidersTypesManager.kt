package com.tangem.blockchainsdk.providers

import com.tangem.blockchainsdk.converters.BlockchainProviderTypesConverter
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [BlockchainProvidersTypesManager] in PROD environment
 *
 * @property blockchainProvidersResponseLoader blockchain providers response loader
 * @property blockchainProviderTypesStore      blockchain provider types store
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class ProdBlockchainProvidersTypesManager @Inject constructor(
    private val blockchainProvidersResponseLoader: BlockchainProvidersResponseLoader,
    private val blockchainProviderTypesStore: BlockchainProviderTypesStore,
) : BlockchainProvidersTypesManager {

    override fun get(): StateFlow<BlockchainProviderTypes> = blockchainProviderTypesStore.get()

    override suspend fun update() {
        val response = blockchainProvidersResponseLoader.load()

        if (response == null) {
            Timber.e("Error loading BlockchainProviderTypes")
            return
        }

        Timber.i("Update BlockchainProviderTypes")

        blockchainProviderTypesStore.store(
            value = BlockchainProviderTypesConverter.convert(response),
        )
    }
}