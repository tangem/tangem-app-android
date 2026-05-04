package com.tangem.blockchainsdk.providers

import com.tangem.blockchainsdk.converters.BlockchainProviderTypesConverter
import kotlinx.coroutines.flow.Flow
import com.tangem.utils.logging.TangemLogger
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

    override fun get(): Flow<BlockchainProviderTypes> = blockchainProviderTypesStore.get()

    override suspend fun update() {
        val response = blockchainProvidersResponseLoader.load()

        if (response == null) {
            TangemLogger.e("Error loading BlockchainProviderTypes")
            return
        }

        TangemLogger.i("Update BlockchainProviderTypes")

        blockchainProviderTypesStore.store(
            value = BlockchainProviderTypesConverter.convert(response),
        )
    }
}